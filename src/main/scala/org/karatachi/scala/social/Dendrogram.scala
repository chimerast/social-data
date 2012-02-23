package org.karatachi.scala.social

import java.awt.image.BufferedImage
import java.awt.Color
import java.awt.RenderingHints
import java.io.FileOutputStream

import scala.math.max
import scala.swing.Dimension
import scala.swing.Graphics2D
import scala.swing.Component
import scala.swing.MainFrame
import scala.swing.ScrollPane
import scala.swing.SimpleSwingApplication
import scala.swing.Swing

import org.karatachi.scala.social.HierarchialClustering.Leaf
import org.karatachi.scala.social.HierarchialClustering.Node
import org.karatachi.scala.social.HierarchialClustering.Tree

import Dendrogram.drawtree
import Dendrogram.getdepth
import Dendrogram.getheight
import javax.imageio.ImageIO

object Dendrogram {
  def apply(tree: Tree, labels: Array[String]): Unit = {
    Swing.onEDT { new Dendrogram(tree, labels) startup (Array[String]()) }
  }

  def save(tree: Tree, labels: Array[String], filename: String): Unit = {
    // 高さと幅
    val h = getheight(tree) * 20
    val w = 1200.0
    val depth = getdepth(tree)

    // 幅は固定されているため、適宜縮尺する
    val scaling = (w - 150).toDouble / depth

    val image = new BufferedImage(w.toInt, h.toInt, BufferedImage.TYPE_3BYTE_BGR)

    val g = image.getGraphics.asInstanceOf[Graphics2D]
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, image.getWidth, image.getHeight)
    drawtree(g, tree, h, scaling, labels)
    g.dispose

    ImageIO.write(image, "png", new FileOutputStream(filename))
  }

  class WrappedGraphics2D(g: Graphics2D) {
    def drawLine(x1: Double, y1: Double, x2: Double, y2: Double): Unit =
      g.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)

    def drawString(str: String, x: Double, y: Double): Unit =
      g.drawString(str, x.toInt, y.toInt)

    def drawStringCenter(str: String, x: Double, y: Double): Unit = {
      val width = g.getFontMetrics().stringWidth(str)
      val height = g.getFontMetrics().getHeight()
      drawString(str, x - width / 2.0, y - height / 2.0)
    }
  }

  implicit def wrapGraphics2D(g: Graphics2D): WrappedGraphics2D = new WrappedGraphics2D(g)

  def drawtree(g: Graphics2D, tree: Tree, h: Double, scaling: Double, labels: Array[String]): Unit = {
    g.setColor(Color.BLUE)
    g.drawLine(0.0, h / 2, 10.0, h / 2)
    drawnode(g, tree, 10, h / 2, scaling, labels)
  }

  def drawnode(g: Graphics2D, tree: Tree, x: Double, y: Double, scaling: Double, labels: Array[String]): Unit = {
    tree match {
      case Node(_, _, left, right, distance) =>
        val h1 = getheight(left) * 20
        val h2 = getheight(right) * 20
        val top = y - (h1 + h2) / 2
        val bottom = y + (h1 + h2) / 2
        // 直線の長さ
        val ll = distance * scaling

        g.setColor(Color.BLUE)

        // クラスタから子への垂直な直線
        g.drawLine(x, top + h1 / 2, x, bottom - h2 / 2)

        // 左側のアイテムへの水平な直線
        g.drawLine(x, top + h1 / 2, x + ll, top + h1 / 2)

        // 右側のアイテムへの水平な直線
        g.drawLine(x, bottom - h2 / 2, x + ll, bottom - h2 / 2)

        drawnode(g, left, x + ll, top + h1 / 2, scaling, labels)
        drawnode(g, right, x + ll, bottom - h2 / 2, scaling, labels)
      case Leaf(id, _) =>
        g.setColor(Color.BLACK)
        // 終点であればアイテムのラベルを描く
        g.drawString(labels(id), x + 5, y + 5)
    }
  }

  def getheight(tree: Tree): Double = {
    tree match {
      case Node(_, _, left, right, distance) =>
        // そうでなければ高さはそれぞれの枝の高さ
        getheight(left) + getheight(right)
      case Leaf(_, _) =>
        // 終端であればたかさは1にする
        1.0
    }
  }

  def getdepth(tree: Tree): Double = {
    tree match {
      case Node(_, _, left, right, distance) =>
        // 枝の距離は二つの方向の大きい方にそれ自身の距離を足したもの
        max(getdepth(left), getdepth(right)) + distance
      case Leaf(_, _) =>
        // 終端への距離は0
        0.0
    }
  }
}

class Dendrogram(tree: Tree, labels: Array[String]) extends SimpleSwingApplication {
  import Dendrogram._

  // 高さと幅
  val h = getheight(tree) * 20
  val w = 1200.0
  val depth = getdepth(tree)

  // 幅は固定されているため、適宜縮尺する
  val scaling = (w - 150).toDouble / depth

  def top = new MainFrame {
    background = Color.WHITE
    resizable = true
    contents = new ScrollPane() {
      preferredSize = new Dimension(w.toInt, 800)
      viewportView = Some(new Component() {
        preferredSize = new Dimension(w.toInt, h.toInt)
        override def paint(g: Graphics2D): Unit = {
          drawtree(g, tree, h, scaling, labels)
        }
      })
    }
  }
}
