package org.karatachi.scala.jung

import java.awt.Dimension
import java.io.BufferedWriter
import java.io.FileWriter
import edu.uci.ics.jung.algorithms.layout.CircleLayout
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.SparseMultigraph
import edu.uci.ics.jung.io.GraphMLWriter
import edu.uci.ics.jung.visualization.BasicVisualizationServer
import javax.swing.JFrame
import edu.uci.ics.jung.graph.DirectedSparseGraph

object JUNGTest extends App {
  val g = new SparseMultigraph[Integer, String]()
  //g.addVertex(1)
  //g.addVertex(2)
  //g.addVertex(3)
  g.addEdge("Edge-A", 1, 2)
  g.addEdge("Edge-B", 2, 3)
  println(g.toString())

  val g2 = new SparseMultigraph[Integer, String]()
  //g2.addVertex(1)
  //g2.addVertex(2)
  //g2.addVertex(3)
  g2.addEdge("Edge-A", 1, 3)
  g2.addEdge("Edge-B", 2, 3, EdgeType.DIRECTED)
  g2.addEdge("Edge-C", 3, 2, EdgeType.DIRECTED)
  g2.addEdge("Edge-P", 2, 3)
  println(g2.toString())

  val out = new BufferedWriter(new FileWriter("/Users/chimera/test.xml"))
  val w = new GraphMLWriter[Integer, String]()
  w.save(g2, out)
  out.close

  // The Layout<V, E> is parameterized by the vertex and edge types
  val layout = new CircleLayout(g)
  layout.setSize(new Dimension(300, 300)) // sets the initial size of the space
  // The BasicVisualizationServer<V,E> is parameterized by the edge types
  val vv =
    new BasicVisualizationServer[Integer, String](layout)
  vv.setPreferredSize(new Dimension(350, 350)) //Sets the viewing area size

  val frame = new JFrame("Simple Graph View")
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  frame.getContentPane().add(vv)
  frame.pack()
  frame.setVisible(true)
}
