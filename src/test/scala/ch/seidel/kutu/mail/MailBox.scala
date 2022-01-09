package ch.seidel.kutu.mail

import jakarta.mail.Address
import jakarta.mail.internet.{AddressException, InternetAddress, MimeMessage}

import java.util
import java.util.{ArrayList, HashMap, HashSet, Map, Set}

object Mailbox {
  private val mailboxes: Map[Address, Mailbox] = new HashMap

  def get(a: Address): Mailbox = {
    mailboxes.computeIfAbsent(a, (a) => new Mailbox(a))
  }

  @throws[AddressException]
  def get(address: String): Mailbox = get(new InternetAddress(address))

  def clearAll(): Unit = {
    mailboxes.clear()
  }
}

class Mailbox(val address: Address) extends ArrayList[MimeMessage] {
  final private val unread: Set[MimeMessage] = new HashSet
  private var error: Boolean = false

  def getAddress: Address = this.address

  def isError: Boolean = this.error

  def setError(error: Boolean): Unit = {
    this.error = error
  }

  def getNewMessageCount: Int = {
    this.unread.retainAll(this)
    this.unread.size
  }

  override def get(msgnum: Int): MimeMessage = {
    val m: MimeMessage = super.get(msgnum)
    this.unread.remove(m)
    m
  }

  override def addAll(messages: util.Collection[_ <: MimeMessage]): Boolean = {
    this.unread.addAll(messages)
    super.addAll(messages)
  }

  override def add(message: MimeMessage): Boolean = {
    this.unread.add(message)
    super.add(message)
  }

  def clearNewStatus(): Unit = {
    this.unread.clear()
  }
}