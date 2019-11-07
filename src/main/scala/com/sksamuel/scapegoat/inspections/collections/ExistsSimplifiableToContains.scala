package com.sksamuel.scapegoat.inspections.collections

import com.sksamuel.scapegoat.{ Inspection, InspectionContext, Inspector, Levels }

/**
 * @author Stephen Samuel
 *
 *         Inspired by Intellij
 */
class ExistsSimplifiableToContains extends Inspection("Exists simplifiable to contains", Levels.Info) {

  def inspector(context: InspectionContext): Inspector = new Inspector(context) {
    override def postTyperTraverser = Some apply new context.Traverser {

      import context.global._

      private val Equals = TermName("$eq$eq")

      private def doesElementTypeMatch(container: Tree, value: Tree): Boolean = {
        val valueType = value.tpe.underlying.typeSymbol.tpe
        val traversableType = container.tpe.underlying.baseType(typeOf[Traversable[Any]].typeSymbol)
        traversableType.typeArgs.exists(t => valueType <:< t || valueType =:= t)
      }

      private def isContainsTraversable(tree: Tree): Boolean = {
        // Traversable itself doesn't include a .contains() method
        isSet(tree) || isSeq(tree) || isList(tree) || isMap(tree)
      }

      override def inspect(tree: Tree): Unit = {
        tree match {
          case Apply(Select(lhs, TermName("exists")), List(Function(_, Apply(Select(_, Equals), List(x)))))
            if isContainsTraversable(lhs) && doesElementTypeMatch(lhs, x) =>
              context.warn(tree.pos, self, "exists(x => x == y) can be replaced with contains(y): " + tree.toString().take(500))
          case _ => continue(tree)
        }
      }
    }
  }
}
