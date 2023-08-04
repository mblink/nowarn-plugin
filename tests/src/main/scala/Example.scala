object TestModifier {
  @unused private val unusedVal1 = ()
  @bl.unused private val unusedVal2 = ()

  @unused private def unusedDef1() = ()
  @bl.unused private def unusedDef2() = ()

  def unusedParam1(@unused x: Unit) = println()
  def unusedParam2(@bl.unused x: Unit) = println()

  @unused private def unusedParamInUnusedDef1(@unused x: Unit) = println()
  @bl.unused private def unusedParamInUnusedDef2(@bl.unused x: Unit) = println()

  def unusedImplicitParam1()(implicit @unused x: Unit) = println()
  def unusedImplicitParam2()(implicit @bl.unused x: Unit) = println()

  @unused private def unusedImplicitParamInUnusedDef1()(implicit @unused x: Unit) = println()
  @bl.unused private def unusedImplicitParamInUnusedDef2()(implicit @bl.unused x: Unit) = println()

  @unused private class UnusedClass1()
  @bl.unused private class UnusedClass2()

  class UnusedParamInClass1(@unused x: Unit)
  class UnusedParamInClass2(@bl.unused x: Unit)

  @unused private class UnusedParamInUnusedClass1(@unused x: Unit)
  @bl.unused private class UnusedParamInUnusedClass2(@bl.unused x: Unit)

  class UnusedValInClass1() { @unused private val x = () }
  class UnusedValInClass2() { @bl.unused private val x = () }

  @unused private class UnusedValInUnusedClass1() { @unused private val x = () }
  @bl.unused private class UnusedValInUnusedClass2() { @bl.unused private val x = () }

  class UnusedDefInClass1() { @unused private def x() = () }
  class UnusedDefInClass2() { @bl.unused private def x() = () }

  @unused private class UnusedDefInUnusedClass1() { @unused private def x() = () }
  @bl.unused private class UnusedDefInUnusedClass2() { @bl.unused private def x() = () }

  @unused private trait UnusedTrait1
  @bl.unused private trait UnusedTrait2

  trait UnusedValInTrait1 { @unused private val x = () }
  trait UnusedValInTrait2 { @bl.unused private val x = () }

  @unused private trait UnusedValInUnusedTrait1 { @unused private val x = () }
  @bl.unused private trait UnusedValInUnusedTrait2 { @bl.unused private val x = () }

  trait UnusedDefInTrait1 { @unused private def x() = () }
  trait UnusedDefInTrait2 { @bl.unused private def x() = () }

  @unused private trait UnusedDefInUnusedTrait1 { @unused private def x() = () }
  @bl.unused private trait UnusedDefInUnusedTrait2 { @bl.unused private def x() = () }

  @unused private object UnusedObject1
  @bl.unused private object UnusedObject2

  object UnusedValInObject1 { @unused private val x = () }
  object UnusedValInObject2 { @bl.unused private val x = () }

  @unused private object UnusedValInUnusedObject1 { @unused private val x = () }
  @bl.unused private object UnusedValInUnusedObject2 { @bl.unused private val x = () }

  object UnusedDefInObject1 { @unused private def x() = () }
  object UnusedDefInObject2 { @bl.unused private def x() = () }

  @unused private object UnusedDefInUnusedObject1 { @unused private def x() = () }
  @bl.unused private object UnusedDefInUnusedObject2 { @bl.unused private def x() = () }

  @unused private type UnusedType1 = Unit
  @bl.unused private type UnusedType2 = Unit
}

object TestAnnotated {
  @deprecated("", "") val x = 1

  def usesDeprecated: Int = x: @undeprecated
  @unused private def usesDeprecatedAndUnused: Int = (x: @undeprecated)
}
