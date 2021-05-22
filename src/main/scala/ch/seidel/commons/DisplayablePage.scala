package ch.seidel.commons

import scalafx.scene.Node

trait DisplayablePage {
  def getPage: Node
  def release(): Unit = {}
}
