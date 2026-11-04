package com.yesmin.ai.utils
/** Small bounded recursive-descent arithmetic evaluator; no scripting engine or eval. */
object Calculator {
 fun calculate(raw: String): String {
  require(raw.length in 1..300) { "Enter 1–300 characters" }
  val s = raw.filterNot(Char::isWhitespace).replace('×','*').replace('÷','/')
  var i = 0; var depth = 0
  fun number(): Double {
   val start=i
   while(i<s.length && (s[i].isDigit() || s[i]=='.')) i++
   require(i>start) { "Expected a number at ${i+1}" }
   return s.substring(start,i).toDouble()
  }
  lateinit var expr: () -> Double
  fun factor(): Double {
   require(++depth <= 30) { "Expression nested too deeply" }
   val value = when {
    i<s.length && s[i]=='+' -> { i++; factor() }
    i<s.length && s[i]=='-' -> { i++; -factor() }
    i<s.length && s[i]=='(' -> { i++; val v=expr(); require(i<s.length && s[i++]==')') { "Missing )" }; v }
    else -> number()
   }
   depth--; return value
  }
  fun term(): Double {
   var v=factor()
   while(i<s.length && s[i] in "*/") {
    val op=s[i++]; val rhs=factor()
    v=if(op=='*') v*rhs else { require(rhs!=0.0) { "Division by zero" }; v/rhs }
   }
   return v
  }
  expr = { var v=term(); while(i<s.length && s[i] in "+-") { val op=s[i++]; val rhs=term(); v=if(op=='+') v+rhs else v-rhs }; v }
  val value=expr(); require(i==s.length && value.isFinite()) { "Invalid expression or overflow" }
  return if(value==value.toLong().toDouble()) value.toLong().toString() else value.toString()
 }
}
