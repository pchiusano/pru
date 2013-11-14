package pru

trait Trampoline[+A] {
  def run: A = Trampoline.run(this)
  def flatMap[B](f: A => Trampoline[B]) = Trampoline.bind(this)(f)
  def map[B](f: A => B): Trampoline[B] =
    flatMap(f andThen Trampoline.done)
}

object Trampoline {
  case class Done[+A](get: A) extends Trampoline[A]
  case class More[+A](force: () => Trampoline[A]) extends Trampoline[A]
  case class Bind[A,+B](force: () => Trampoline[A],
                        f: A => Trampoline[B]) extends Trampoline[B]
  @annotation.tailrec
  def run[A](t: Trampoline[A]): A = t match {
    case Done(a) => a
    case More(k) => run(k())
    case Bind(force, f) => run(force() flatMap f)
  }

  def unit[A](a: => A) = Done(a)

  def bind[A,B](a: Trampoline[A])(f: A => Trampoline[B]): Trampoline[B] =
    a match {
      case Done(forced) => More(() => f(forced))
      case More(force) => Bind(force, f)
      case Bind(force,g) => More(() => Bind(force, g andThen (_ flatMap f)))
    }
  def more[A](a: => Trampoline[A]): Trampoline[A] =
    More(() => a)
  def delay[A](a: => A): Trampoline[A] =
    More(() => Done(a))
  def done[A](a: A): Trampoline[A] = Done(a)
}
