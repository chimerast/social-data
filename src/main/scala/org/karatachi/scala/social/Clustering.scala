package org.karatachi.scala.social

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.math._
import edu.uci.ics.jung.graph.Graph

object Distance {
  type T = Array[Double]

  def pearson(a1: T, a2: T): Double = {
    val v = a1 zip a2 filter (e => !e._1.isNaN && !e._2.isNaN)

    val v1 = v.map(_._1)
    val v2 = v.map(_._2)

    val n = v.size

    // 単純な合計
    val sum1 = v1.sum
    val sum2 = v2.sum

    // 平方を合計
    val sum1Sq = v1.map(pow(_, 2)).sum
    val sum2Sq = v2.map(pow(_, 2)).sum

    // 積の合計
    val pSum = v1.zip(v2).map { case (i1, i2) => i1 * i2 }.sum

    // ピアソンによるスコアを算出
    val num = pSum - (sum1 * sum2 / n)
    val den = sqrt((sum1Sq - pow(sum1, 2) / n) * (sum2Sq - pow(sum2, 2) / n))
    if (den == 0.0) return 0.0

    1.0 - num / den
  }

  def tanimoto(v1: T, v2: T): Double = {
    val c1 = v1.filter(0.0!=).size
    val c2 = v2.filter(0.0!=).size
    val shr = (v1 zip v2).filter { case (i1, i2) => i1 != 0.0 && i2 != 0.0 }.size

    1.0 - (shr.toDouble / (c1 + c2 - shr))
  }
}

object HierarchialClustering {
  type T = Array[Double]
  type Distance = (T, T) => Double

  abstract case class Tree(id: Int, vec: T)
  case class Node(override val id: Int, override val vec: T, left: Tree, right: Tree, distance: Double) extends Tree(id, vec)
  case class Leaf(override val id: Int, override val vec: T) extends Tree(id, vec)
  object EmptyTree extends Tree(0, Array[Double]())

  def clustering(rows: Array[T], distance: Distance): Tree = {
    val distances = Map[(Int, Int), Double]()
    var nodeId = -1

    // クラスタは最初は行たち
    val clusters = ArrayBuffer[Tree](rows.zipWithIndex.map { case (row, i) => Leaf(i, row) }: _*)

    case class Selected(ci: Tree, cj: Tree, i: Int, j: Int, d: Double)

    while (clusters.size > 1) {
      var selected = Selected(EmptyTree, EmptyTree, -1, -1, Double.MaxValue)

      // すべての組をループし、最も距離の近い組を探す
      clusters.zipWithIndex.combinations(2).foreach {
        case ArrayBuffer((ci, i), (cj, j)) =>
          // 距離をキャッシュしてあればそれを使う
          val d = distances.getOrElseUpdate((ci.id, cj.id), distance(ci.vec, cj.vec))
          if (d < selected.d)
            selected = Selected(ci, cj, i, j, d)
      }

      // 二つのクラスタの平均を計算する
      val merged = (selected.ci.vec zip selected.cj.vec).map { case (i1, i2) => (i1 + i2) / 2.0 }

      // 新たなクラスタを作る
      val newcluster = Node(nodeId, merged, selected.ci, selected.cj, selected.d)
      // 元のセットではないクラスタのIDは負にする
      nodeId -= 1

      clusters.remove(selected.j)
      clusters.remove(selected.i)
      clusters.append(newcluster)
    }

    clusters(0)
  }

  def graph(t: Tree, g: Graph[Tree, String]): Unit = {
    t match {
      case n: Node =>
        g.addEdge(n.id + "_" + n.left.id, n, n.left)
        g.addEdge(n.id + "_" + n.right.id, n, n.right)
        graph(n.left, g)
        graph(n.right, g)
      case _ =>
    }
  }

  def print(t: Tree, f: (Int) => String): Unit =
    print(t, 0, f)

  private def print(t: Tree, i: Int, f: (Int) => String): Unit = {
    t match {
      case n: Node =>
        print(n.left, i + 1, f)
        print(n.right, i + 1, f)
      case l: Leaf =>
        println((" " * i) + f(l.id))
    }
  }
}

object KMeansClustering {
  type T = Array[Double]
  type Distance = (T, T) => Double

  def clustering(rows: Array[T], distance: Distance, k: Int, n: Int = 100): Array[Array[Int]] = {
    val centers = 0 until k
    val indices = rows(0).indices

    // それぞれのポイントの最小値と最大値を決める
    val ranges = indices.map { i => (rows.map(_(i)).min, rows.map(_(i)).max) }.toArray
    // 重心をランダムにk個配置する
    val clusters = centers.map { j => ranges.map { case (min, max) => random * (max - min) + min }.toArray }.toArray

    var lastmatches = Array[ArrayBuffer[Int]]()

    for (t <- 0 until n) {
      println("Iteration %d" format (t))
      val bestmatches = centers.map(i => ArrayBuffer[Int]()).toArray

      // それぞれの行に対して、もっとも近い重心を探し出す
      rows.zipWithIndex.foreach {
        case (row, j) =>
          val bestmatch = clusters.zipWithIndex.minBy { case (cluster, _) => distance(cluster, row) }._2
          bestmatches(bestmatch) += j
      }

      // 結果が前回と同じであれば終了
      if (bestmatches.sameElements(lastmatches))
        return lastmatches.map(_.toArray)

      // 重心をそのメンバーの平均に移動する
      centers.foreach { i =>
        val cluster = bestmatches(i).map(rows)
        if (cluster.size > 0)
          clusters(i) = indices.map { j => cluster.map(_(j)).sum / cluster.size }.toArray
      }

      lastmatches = bestmatches
    }

    lastmatches.map(_.toArray)
  }
}
