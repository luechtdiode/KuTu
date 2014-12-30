package ch.seidel

object CodeGen extends App {
  scala.slick.model.codegen.SourceCodeGenerator.main(
    Array(
        "scala.slick.driver.MySQLDriver",
        "com.mysql.jdbc.Driver",
        "jdbc:mysql://localhost:3306/kutu",
        "C:/Users/Roland/git/KuTu/src/test/scala",
        "ch.seidel.domain", "kutu", "kutu")
  )
}