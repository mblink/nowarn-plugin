package bl

import scala.reflect.internal.util.TransparentPosition
import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.TypingTransformers

class NowarnPlugin(override val global: Global) extends Plugin { self =>
  import global._

  val name: String = "nowarn"
  val description: String = "Expands custom annotations into `@annotation.nowarn`"

  @volatile private var configuredAnnotations = Map.empty[Tree, List[String]]

  override def init(opts: List[String], error: String => Unit): Boolean = {
    opts.foreach(opt => opt.split(':').toList match {
      case annName :: wConf :: Nil =>
        val annTree: Tree = annName.split('.').filterNot(_.isEmpty).toList match {
          case h :: Nil => Ident(TypeName(h))
          case (h :: t) :+ last => Select(t.foldLeft(Ident(TermName(h)): Tree)((acc, x) => Select(acc, TermName(x))), TypeName(last))
          case _ => error(s"nowarn: invalid option: `$opt`"); q""
        }
        self.synchronized {
          val (found, upd) = configuredAnnotations.foldLeft((false, Map.empty[Tree, List[String]])) {
            case ((_, accAnns), (t, wConfs)) if t.equalsStructure(annTree) => (true, accAnns + (t -> (wConf :: wConfs)))
            case ((accFound, accAnns), (t, wConfs)) => (accFound, accAnns + (t -> wConfs))
          }
          configuredAnnotations = if (found) upd else upd + (annTree -> List(wConf))
        }
      case _ => error(s"nowarn: invalid option `$opt`")
    })
    true
  }

  private def withAllPos(tree: Tree, pos: Position): Tree = {
    tree.foreach { t =>
      if (!t.pos.isDefined || t.pos == NoPosition)
        t.setPos(new TransparentPosition(pos.source, pos.start, pos.end, pos.end))
    }
    tree
  }

  private def nowarnAnnotation(tree: Tree)(wConf: String): Tree =
    withAllPos(q"""new _root_.scala.annotation.nowarn(${Literal(Constant(wConf))})""", tree.pos)

  private def addNowarnAnnotations(tree: Tree, wConfs: List[String], mods: Modifiers): Modifiers =
    mods.mapAnnotations(wConfs.map(nowarnAnnotation(tree)) ::: _)

  object ConfiguredAnnotation {
    def unapply(tree: Tree): Option[List[String]] =
      tree match {
        case Apply(Select(New(ann), _), _) =>
          configuredAnnotations.collectFirst { case (t, wConfs) if t.equalsStructure(ann) => wConfs }

        case _ => None
      }
  }

  object ModifierAnnotation {
    def unapply(mods: Modifiers): Option[Tree => Modifiers] = {
      val (wConfs, remAnns) = mods.annotations.foldRight((List.empty[String], List.empty[Tree])) {
        case (ConfiguredAnnotation(wConfs), (accWConfs, accAnns)) => (wConfs ::: accWConfs, accAnns)
        case (ann, (accWConfs, accAnns)) => (accWConfs, ann.duplicate :: accAnns)
      }
      wConfs match {
        case Nil => None
        case _ :: _ => Some(addNowarnAnnotations(_, wConfs, mods.mapAnnotations(_ => remAnns)))
      }
    }
  }

  object AnnotatedTree {
    private val pf: PartialFunction[Tree, Tree] = {
      case t @ ClassDef(ModifierAnnotation(f), name, tparams, impl) => treeCopy.ClassDef(t, f(t), name, tparams, impl)
      case t @ DefDef(ModifierAnnotation(f), name, tparams, vparamss, tpt, rhs) => treeCopy.DefDef(t, f(t), name, tparams, vparamss, tpt, rhs)
      case t @ ModuleDef(ModifierAnnotation(f), name, impl) => treeCopy.ModuleDef(t, f(t), name, impl)
      case t @ TypeDef(ModifierAnnotation(f), name, tparams, rhs) => treeCopy.TypeDef(t, f(t), name, tparams, rhs)
      case t @ ValDef(ModifierAnnotation(f), name, tpt, rhs) => treeCopy.ValDef(t, f(t), name, tpt, rhs)
      case t @ Annotated(ann @ ConfiguredAnnotation(wConf :: wConfs), arg) =>
        wConfs.foldLeft(treeCopy.Annotated(t, nowarnAnnotation(ann)(wConf), arg)) {
          case (t @ Annotated(ann, arg), wConf) => treeCopy.Annotated(t, nowarnAnnotation(ann)(wConf), arg)
        }
    }

    def unapply(tree: Tree): Option[Tree] = pf.lift(tree)
  }

  private lazy val phase = new PluginComponent with TypingTransformers {
    override val phaseName: String = NowarnPlugin.this.name
    override val global: NowarnPlugin.this.global.type = NowarnPlugin.this.global
    override final def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit = newTransformer(unit).transformUnit(unit)
    }

    private def newTransformer(unit: CompilationUnit) =
      new TypingTransformer(unit) {
        override def transform(tree: Tree): Tree =
          super.transform(tree) match {
            case AnnotatedTree(t) => t
            case t => t
          }
      }

    override val runsAfter: List[String] = List("parser")
    override val runsBefore: List[String] = List("namer")
  }

  override lazy val components: List[PluginComponent] = List(phase)
}
