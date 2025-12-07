package ch.seidel.kutu.mail

import jakarta.mail.Address
import jakarta.mail.internet.{AddressException, InternetAddress, MimeMessage}

import java.util
import java.util.{ArrayList, HashMap, HashSet, Map, Set}

object Mailbox {
  private val mailboxes: util.Map[Address, Mailbox] = new util.HashMap

  def get(a: Address): Mailbox = {
    mailboxes.computeIfAbsent(a, (a) => new Mailbox(a))
  }

  @throws[AddressException]
  def get(address: String): Mailbox = get(new InternetAddress(address))

  def clearAll(): Unit = {
    mailboxes.clear()
  }
}

class Mailbox(val address: Address) extends util.ArrayList[MimeMessage] {
  final private val unread: util.Set[MimeMessage] = new util.HashSet
  private var error: Boolean = false

  def getAddress: Address = this.address

  def isError: Boolean = this.error

  def setError(error: Boolean): Unit = {
    println(s"mailbox error")
    this.error = error
  }

  def getNewMessageCount: Int = {
    this.unread.retainAll(this)
    this.unread.size
  }

  override def get(msgnum: Int): MimeMessage = {
    println(s"get $msgnum message from mailbox")
    val m: MimeMessage = super.get(msgnum)
    this.unread.remove(m)
    m
  }

  override def addAll(messages: util.Collection[? <: MimeMessage]): Boolean = {
    println(s"add messages to mailbox")
    this.unread.addAll(messages)
    super.addAll(messages)
  }

  override def add(message: MimeMessage): Boolean = {
    println(s"add message to mailbox")
    this.unread.add(message)
    super.add(message)
  }

  def clearNewStatus(): Unit = {
    this.unread.clear()
  }
}