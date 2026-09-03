package com.example

import com.example.ai.HinglishCommandParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLocalWakeWordResponse_doesNotContainNamaste_andAddressesShoaib() {
    val action = HinglishCommandParser.parseLocally("hey assistant")
    assertNotNull(action)
    assertTrue(action!!.responseSpeech.contains("Shoaib bhai"))
    assertFalse(action.responseSpeech.contains("Namaste", ignoreCase = true))
    assertFalse(action.responseSpeech.contains("Namaskar", ignoreCase = true))
  }

  @Test
  fun testHotspotVoiceControl() {
    val action = HinglishCommandParser.parseLocally("hotspot off kar")
    assertNotNull(action)
    assertEquals("SETTINGS", action!!.type)
    assertEquals("hotspot", action.target)
    assertEquals("off", action.details)
    assertTrue(action.responseSpeech.contains("Shoaib bhai"))
    assertFalse(action.responseSpeech.contains("Namaste", ignoreCase = true))
  }

  @Test
  fun testOpenAppVoiceControl() {
    val action = HinglishCommandParser.parseLocally("instagram open kar")
    assertNotNull(action)
    assertEquals("OPEN_APP", action!!.type)
    assertEquals("instagram", action.target)
    assertTrue(action.responseSpeech.contains("Shoaib bhai"))
  }

  @Test
  fun testObedienceAndKnowledgeVoiceResponse() {
    val action = HinglishCommandParser.parseLocally("ye meri saari baat mane or is ke pass sab jan kari ho")
    assertNotNull(action)
    assertEquals("INFO", action!!.type)
    assertTrue(action.responseSpeech.contains("Shoaib bhai"))
    assertFalse(action.responseSpeech.contains("Namaste", ignoreCase = true))
  }
}
