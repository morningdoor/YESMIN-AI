package com.yesmin.ai
import com.yesmin.ai.utils.Calculator
import com.yesmin.ai.coding.CodeBlocks
import org.junit.Assert.*
import org.junit.Test
class CalculatorTest {
 @Test fun precedence() {assertEquals("14",Calculator.calculate("2 + 3 * 4"))}
 @Test fun parentheses() {assertEquals("30",Calculator.calculate("(12 + 3) * 4 / 2"))}
 @Test fun unary() {assertEquals("-10",Calculator.calculate("-(2+3)*2"))}
 @Test fun unicodeOperators() {assertEquals("12",Calculator.calculate("6 × 4 ÷ 2"))}
 @Test fun divisionByZeroRejected() {assertTrue(runCatching{Calculator.calculate("1/0")}.isFailure)}
 @Test fun scriptingRejected() {assertTrue(runCatching{Calculator.calculate("Runtime.exec('ls')")}.isFailure)}
 @Test fun unbalancedRejected() {assertTrue(runCatching{Calculator.calculate("(1+2")}.isFailure)}
 @Test fun codeExtractedWithoutExecution() {val b=CodeBlocks.extract("Text\n```kotlin\nval x = 1\n``` ending").single();assertEquals("kotlin",b.language);assertEquals("val x = 1",b.code)}
}
