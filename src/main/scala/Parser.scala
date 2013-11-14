package pru.read

import pru.Trampoline
import Parser.{Failure,Fatal,Nonfatal}

case class Parser[+A](run: String => Trampoline[Either[Failure,(A, Int)]]) {

  def apply(s: String): Either[Failure, A] =
    run(s).map(_.right.map(_._1)).run

  def map[B](f: A => B): Parser[B] = Parser { s =>
    run(s).map {
      case Right((a, nRead)) => Right(f(a) -> nRead)
      case Left(e) => Left(e)
    }
  }

  def flatMap[B](f: A => Parser[B]): Parser[B] = Parser { s =>
    run(s) flatMap {
      case Right((a, nRead)) => f(a).run(s.drop(nRead))
      case Left(e) => Trampoline.done(Left(e))
    }
  }

  def commit: Parser[A] = Parser { s =>
    run(s).map {
      case Left(Nonfatal(msgs)) => Left(Fatal(msgs))
      case a => a
    }
  }

  def nonEmpty: Parser[A] = Parser { s =>
    run(s).map {
      case Right((a, nRead)) =>
        if (nRead == 0) Left(Nonfatal(List("parse must be nonempty")))
        else Right(a -> nRead)
      case a => a
    }
  }

  def or[B>:A](p2: => Parser[B]): Parser[B] = {
    lazy val other = p2
    Parser { s => run(s).flatMap {
      case Left(Nonfatal(_)) => other.run(s)
      case a => Trampoline.done(a)
    }}
  }

  def filter(f: A => Boolean): Parser[A] = Parser { s =>
    run(s).map {
      case r@Right((a, _)) =>
        if (f(a)) r
        else Left(Nonfatal(List("did not pass filter")))
      case a => a
    }
  }
}

object Parser {
  def pure[A](a: => A): Parser[A] =
    Parser { s => Trampoline.done(Right(a -> 0)) }
  def bind[A,B](a: Parser[A])(f: A => Parser[B]): Parser[B] =
    a flatMap f
  def toChar(c: Char): Parser[String] = Parser { s =>
    val i = s.indexOf('\n')
    if (i >= 0) Trampoline.done(Right(s.substring(0, i+1) -> (i+1)))
    else Trampoline.done(Left(Nonfatal(List(s"expected '$c'"))))
  }

  def toCharOrEOF(c: Char): Parser[String] = Parser { s =>
    val i = s.indexOf('\n')
    if (i >= 0) Trampoline.done(Right(s.substring(0, i+1) -> (i+1)))
    else Trampoline.done(Right(s -> s.length))
  }

  val toEOL: Parser[String] = toChar('\n')

  def toCharToken(s: String)(c: Char): Parser[String] =
    toChar(c).filter(_ == s)

  def scope[A](msg: String)(p: Parser[A]): Parser[A] = Parser { s =>
    p.run(s).map {
      case Left(Nonfatal(msgs)) => Left(Nonfatal(msg :: msgs))
      case Left(Fatal(msgs)) => Left(Fatal(msg :: msgs))
      case a => a
    }
  }

  def trim(p: Parser[String]): Parser[String] = p.map(_.trim)

  def whitespace: Parser[String] = Parser { s =>
    Trampoline.delay {
      var i = 0
      while (i < s.length && s.charAt(i).isWhitespace) i += 1
      Right(s.substring(0, i) -> ((i-1) max 0))
    }
  }

  def whitespace1: Parser[String] = whitespace.nonEmpty

  def fail(msg: String): Parser[Nothing] =
    Parser { _ => Trampoline.done(Left(Nonfatal(List(msg)))) }

  trait Failure extends Throwable
  case class Nonfatal(stack: List[String]) extends Failure
  case class Fatal(stack: List[String]) extends Failure
}

object SSE {

  import Parser._

  case class Stanza[+E,+D](event: E, data: D)

  /**
   * `keyValueLine("foo")` parses `"foo: <blah>"`,
   * returning `<blah>` as its result.
   */
  def keyValueLine(keyName: String): Parser[String] =
    toCharToken(keyName)(':') flatMap { _ => trim(toEOL) }

  /**
   * Parse a 'stanza' of the SSE format:
   *
   * event: blah
   * data: blah-data
   */
  def stanza: Parser[Stanza[String,String]] = for {
    e <- keyValueLine("event")
    d <- keyValueLine("data")
  } yield Stanza(e, d)
}
