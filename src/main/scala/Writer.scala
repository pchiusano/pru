package pru.write

import scala.xml.Utility.{escape => esc}
import Writer._

case class Action(f: StringBuilder => Unit) extends (StringBuilder => Unit) {
  def ++(other: Action): Action =
    Action(sb => { apply(sb); other(sb) })
  def apply(sb: StringBuilder): Unit = f(sb)

  override def toString: String =
    { val sb = new StringBuilder; apply(sb); sb.toString }
}

object Writer {
  type Writer[-A] = A => Action

  implicit def toAction(f: StringBuilder => Unit): Action = Action(f)

  def any(a: Any): Action = Action(sb => sb append a.toString)
  def k(s: String): Action = Action(sb => sb append s)
  def id: Action = Action(sb => ())
  def rep[A](t: Traversable[A])(f: Writer[A]): Action = rep(f)(t)
  def rep[A](f: Writer[A]): Writer[TraversableOnce[A]] = as => Action(sb => as.foreach(x => f(x)(sb)))
  def concat(as: TraversableOnce[Action]): Action = Action(sb => as.foreach(f => f(sb)))
  def intersperse(delim: Action)(as: TraversableOnce[Action]) = Action { sb =>
    var between = false
    as.foreach { a =>
      if (between) { delim(sb); a(sb) }
      else { a(sb); between = true }
    }
  }
  def Intersperse(a: Action)(as: Action*) = intersperse(a)(as)
  def Rep[A](t: A*)(f: Writer[A]): Action = rep(f)(t)
  def Concat(as: TraversableOnce[Action]): Action = concat(as)
}

object XML {

  def escape(s: String): Action = Action(sb => sb append esc(s))
  def literal(s: String): Action = k("\"") ++ escape(s) ++ k("\"")

  def attributes(attrs: TraversableOnce[(String, String)]): Action =
    Action(sb => attrs.foreach { case (x, y) => sb append (" " + x + "=\"" + escape(y) + "\"") })

  def openTag(tag: String, attrs: TraversableOnce[(String, String)]): Action =
    k("<") ++ k(tag) ++ attributes(attrs) ++ k(">")

  def leafTag(tag: String, attrs: TraversableOnce[(String, String)]): Action =
    k("<") ++ k(tag) ++ attributes(attrs) ++ k("/>")

  def closeTag(tag: String): Action = k ("</" + tag + ">")

  def nest[A](tag: String, attributes: TraversableOnce[(String, String)])(inner: TraversableOnce[Action]): Action =
    openTag(tag, attributes) ++ concat(inner) ++ closeTag(tag)

  def Attributes(attrs: (String, String)*): Action = attributes(attrs)
  def OpenTag(tag: String, attrs: (String, String)*): Action = openTag(tag, attrs)
  def LeafTag(tag: String, attrs: (String, String)*): Action = leafTag(tag, attrs)
  def Nest[A](tag: String, attrs: (String, String)*)(inner: Action*): Action = nest(tag, attrs)(inner)
}

object JSON {
  def literal(s: String): Action = XML.literal(s)
  def entry(key: String, value: Action): Action =
    literal(key) ++ k(" : ") ++ value
  def obj(entries: TraversableOnce[(String, Action)]): Action =
    k("{ ") ++ intersperse(k(", "))(entries.map(p => entry(p._1, p._2))) ++ k(" }")
  def list(values: TraversableOnce[Action]): Action =
    k("[") ++ intersperse(k(", "))(values) ++ k("]")
  def Obj(entries: (String, Action)*): Action = obj(entries)
  def List(values: Action*): Action = list(values)
}
