package com.yesmin.ai.coding
data class CodeBlock(val language: String, val code: String)
object CodeBlocks {
 fun extract(text: String): List<CodeBlock> = Regex("```([^\\n`]*)\\n([\\s\\S]*?)```").findAll(text)
  .map { CodeBlock(it.groupValues[1].trim(), it.groupValues[2].trimEnd()) }.toList()
}
