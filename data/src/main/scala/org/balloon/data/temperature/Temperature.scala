package org.balloon.data.temperature

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

trait TemperatureScale

trait Temperature {
  val value: Int

  def to[T <: TemperatureScale : TypeTag]: Temperature
}

case class Celsius(value: Int) extends TemperatureScale with Temperature {
  override def to[T <: TemperatureScale : universe.TypeTag]: Temperature = typeOf[T] match {
    case t if t =:= typeOf[Celsius] => this
    case t if t =:= typeOf[Kelvin] => Kelvin(Math.round(value + 273.15).toInt)
    case t if t =:= typeOf[Fahrenheit] => Fahrenheit(Math.round((value * 1.8) + 32).toInt)
  }
}

case class Kelvin(value: Int) extends TemperatureScale with Temperature {
  override def to[T <: TemperatureScale : universe.TypeTag]: Temperature = typeOf[T] match {
    case t if t =:= typeOf[Kelvin] => this
    case t if t =:= typeOf[Celsius] => Celsius(Math.round(value - 273.15).toInt)
    case t if t =:= typeOf[Fahrenheit] => Fahrenheit(Math.round(((value * 9) / 5) - 459.67).toInt)
  }
}

case class Fahrenheit(value: Int) extends TemperatureScale with Temperature {
  override def to[T <: TemperatureScale : universe.TypeTag]: Temperature = typeOf[T] match {
    case t if t =:= typeOf[Fahrenheit] => this
    case t if t =:= typeOf[Celsius] => Celsius(Math.round((value - 32) / 1.8).toInt)
    case t if t =:= typeOf[Kelvin] => Kelvin(Math.round(((value + 459.67) * 5) / 9).toInt)
  }
}
