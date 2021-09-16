package bl

import scala.reflect.internal.util.TransparentPosition
import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.TypingTransformers

class NowarnPlugin(override val global: Global) extends Plugin { self =>
  import global._

  val name: String = "nowarn"
  val description: String = "Expands custom annotations into `@annotation.nowarn`"

  @volatile var configuredAnnotations = List.empty[(Tree, String)]

  override def init(opts: List[String], error: String => Unit): Boolean = {
    opts.foreach(opt => opt.split(':').toList match {
      case annName :: wConf :: Nil =>
        val annTree: Tree = annName.split('.').filterNot(_.isEmpty).toList match {
          case h :: Nil => Ident(TypeName(h))
          case (h :: t) :+ last => Select(t.foldLeft(Ident(TermName(h)): Tree)((acc, x) => Select(acc, TermName(x))), TypeName(last))
          case _ => error(s"nowarn: invalid option: `$opt`"); q""
        }
        self.synchronized { configuredAnnotations = (annTree, wConf) :: configuredAnnotations }
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

  private def ann(tree: Tree, wConf: String): Tree =
    withAllPos(q"""new _root_.scala.annotation.nowarn(${Literal(Constant(wConf))})""", tree.pos)

  private def addAnn(tree: Tree, wConf: String, mods: Modifiers): Modifiers =
    mods.mapAnnotations(ann(tree, wConf) :: _)

  object ConfiguredAnnotation {
    def unapply(tree: Tree): Option[String] =
      tree match {
        case Apply(Select(New(ann), _), _) =>
          configuredAnnotations.collectFirst { case (t, s) if t.equalsStructure(ann) => s}

        case _ => None
      }
  }

  object ModifierAnnotation {
    def unapply(mods: Modifiers): Option[Tree => Modifiers] = {
      val (confO, rest) = mods.annotations.foldRight((Option.empty[String], List.empty[Tree])) {
        case (ConfiguredAnnotation(conf), (_, anns)) => (Some(conf), anns)
        case (a, (o, anns)) => (o, a.duplicate :: anns)
      }
      confO.map(c => addAnn(_, c, mods.mapAnnotations(_ => rest)))
    }
  }

  object AnnotatedTree {
    private val pf: PartialFunction[Tree, Tree] = {
      case t @ ClassDef(ModifierAnnotation(f), name, tparams, impl) => treeCopy.ClassDef(t, f(t), name, tparams, impl)
      case t @ DefDef(ModifierAnnotation(f), name, tparams, vparamss, tpt, rhs) => treeCopy.DefDef(t, f(t), name, tparams, vparamss, tpt, rhs)
      case t @ ModuleDef(ModifierAnnotation(f), name, impl) => treeCopy.ModuleDef(t, f(t), name, impl)
      case t @ TypeDef(ModifierAnnotation(f), name, tparams, rhs) => treeCopy.TypeDef(t, f(t), name, tparams, rhs)
      case t @ ValDef(ModifierAnnotation(f), name, tpt, rhs) => treeCopy.ValDef(t, f(t), name, tpt, rhs)
      case t @ Annotated(ConfiguredAnnotation(conf), arg) => treeCopy.Annotated(t, ann(t, conf), arg)
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
