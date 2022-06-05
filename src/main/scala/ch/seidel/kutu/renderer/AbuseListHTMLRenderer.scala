package ch.seidel.kutu.renderer

import ch.seidel.kutu.http.AbuseHandler
import ch.seidel.kutu.renderer.PrintUtil.escaped

trait AbuseListHTMLRenderer {

  private val intro = """<!DOCTYPE html><html>
    <head>
      <meta charset="UTF-8" />
      <style type="text/css">
        body {
          font-family: "Arial", "Verdana", sans-serif;
        }
        table {
          border-collapse:collapse;
          border-spacing:1;
          border: 1px solid black;
        }
        tr {
          border: 1px solid grey;
        }
        td {
          padding: 2px;
          border: 1px solid grey;
        }
      </style>
    </head>
    <body>
  """

  private val outro = """
    </td></tr>
    </table>
    </body>
    </html>
  """

  def abusedClientsToHTMListe(): String = {
    val abusedClients = AbuseHandler.getAbusedClients
    abusedClients
      .toList.sortBy(client => s"${client.abused}:${client.path}")
      .zipWithIndex
      .map { item =>
        val client = item._1
        val cid = client.cid.split("@")(0) + " : " + (client.cid + ":").split(":")(1)
        s"${item._2 + 1}</td><td>${client.ip}</td><td>${escaped(cid)}</td><td>${escaped(client.path)}</td><td>${client.abused}"
      }
      .mkString(
        s"$intro<h1>Abused clients</h1><h2>${AbuseHandler.getAbusedClientsCount} abused clients, ${AbuseHandler.getAbusedWatchlistClientsCount} clients in observation</h2><table><tr><td>#</td><td>IP</td><td>CID</td><td>Path</td><td>Abused</td></tr><tr><td>",
        "</td></tr><tr><td>",
        outro)
  }
}
