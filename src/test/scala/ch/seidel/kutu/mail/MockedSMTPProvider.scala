package ch.seidel.kutu.mail

import org.jvnet.mock_javamail.MockTransport

import javax.mail.Provider

class MockedSMTPProvider extends Provider(Provider.Type.TRANSPORT, "mocked", classOf[MockTransport].getName, "Mock", null) {
  System.getProperties.put("mail.transport.protocol.rfc822", "mocked")
}