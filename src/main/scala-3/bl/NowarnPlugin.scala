package bl

import dotty.tools.dotc.printing.ReplPrinter
import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.ast.untpd.*
import dotty.tools.dotc.ast.Trees.Untyped
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.{ctx, Context}
import dotty.tools.dotc.core.Decorators.show
import dotty.tools.dotc.core.Names.{termName, typeName}
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.parsing.Parser
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.typer.TyperPhase

class NowarnPlugin extends StandardPlugin { self =>
  override val name: String = "nowarn"
  override val description: String = "Expands custom annotations into `@annotation.nowarn`"

  @volatile private var configuredAnnotations = Map.empty[String, List[String]]

  override def init(options: List[String]): List[PluginPhase] = {
    options.foreach(opt => opt.split(':').toList match {
      case annName :: wConf :: Nil =>
        self.synchronized {
          configuredAnnotations = configuredAnnotations.updatedWith(annName) {
            case Some(ex) => Some(wConf :: ex)
            case None => Some(List(wConf))
          }
        }
      case _ => System.err.println(s"nowarn: invalid option `$opt`")
    })

    List(phase)
  }

  private def nowarnAnnotation(tree: Tree)(wConf: String)(using Context): Tree =
    Apply(
      Select(
        New(
          Select(
            Select(
              Select(
                Ident(nme.ROOTPKG),
                termName("scala"),
              ),
              termName("annotation"),
            ),
            typeName("nowarn"),
          )
        ),
        nme.CONSTRUCTOR,
      ),
      List(Literal(Constant(wConf))),
    ).withSpan(tree.span)

  object ConfiguredAnnotation {
    def unapply(tree: Tree)(using Context): Option[List[String]] =
      tree match {
        // TODO - use of ReplPrinter is probably not ideal but it works, it's needed to prevent `.show` adding ANSI colors
        // alternatively, uncommenting the line below fixes it for single word annotations, but not sure about all edge cases
        // case Apply(Select(New(Ident(ann)), _), _) => configuredAnnotations.get(ann.show)
        case Apply(Select(New(ann), _), _) => configuredAnnotations.get(ann.toText(new ReplPrinter(ctx)).show)
        case _ => None
      }
  }

  private def replaceAnns(tree: DefTree)(using Context): Tree = {
    val (wConfs, remAnns) = tree.mods.annotations.foldRight((List.empty[String], List.empty[Tree])) {
      case (ConfiguredAnnotation(wConfs), (accWConfs, accAnns)) => (wConfs ::: accWConfs, accAnns)
      case (ann, (accWConfs, accAnns)) => (accWConfs, ann :: accAnns)
    }
    wConfs match {
      case Nil => tree
      case _ :: _ => tree.withAnnotations(wConfs.map(nowarnAnnotation(tree)) ::: remAnns)
    }
  }

  private object phase extends PluginPhase {
    override val phaseName: String = self.name

    override val runsAfter: Set[String] = Set(Parser.name)
    override val runsBefore: Set[String] = Set(TyperPhase.name)

    private object transformer extends UntypedTreeMap(dotty.tools.dotc.ast.untpd.cpy) {
      private def replaceAnns(tree: DefTree)(using Context): Tree = super.transform(self.replaceAnns(tree))

      override def transform(tree: Tree)(using Context): Tree =
        tree match {
          case t @ DefDef(_, _, _, _) => replaceAnns(t)
          case t @ ModuleDef(_, _) => replaceAnns(t)
          case t @ TypeDef(_, _) => replaceAnns(t)
          case t @ ValDef(_, _, _) => replaceAnns(t)
          case t @ Annotated(arg, ann @ ConfiguredAnnotation(wConf :: wConfs)) =>
            super.transform(wConfs.foldLeft(cpy.Annotated(t)(arg, nowarnAnnotation(ann)(wConf))) {
              case (t @ Annotated(arg, ann), wConf) => cpy.Annotated(t)(arg, nowarnAnnotation(ann)(wConf))
            })
          case _ => super.transform(tree)
        }
    }

    override def runOn(units: List[CompilationUnit])(using Context): List[CompilationUnit] =
      super.runOn(units.map { u =>
        u.untpdTree = transformer.transform(u.untpdTree)
        u
      })
  }
}
