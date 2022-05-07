package org.d7z.lang

class LangRunner(
    private val expressions: List<LangCompiler.ExecuteExpression>
) {
    fun ruleVariables() = expressions.flatMap {
        val listOf = mutableListOf<LangCompiler.VariableExecuteVariable>()
        if (it.left is LangCompiler.VariableExecuteVariable) {
            listOf.add(it.left)
        }
        if (it.right is LangCompiler.VariableExecuteVariable) {
            listOf.add(it.right)
        }
        listOf
    }

    fun tryExecute(variables: Map<String, LangDataType>): LangDataType {
        val copy = expressions.map { it.copy() }.toMutableList()
        val typeMap = LinkedHashMap<Int, LangDataType>()
        fun LangCompiler.ExecuteExpression.loadOnesType(
            master: LangCompiler.IExecuteVariable,
            other: LangCompiler.IExecuteVariable
        ) {
            val masterType = when (master) {
                is LangCompiler.VariableExecuteVariable -> variables[master.name]!!
                is LangCompiler.ConstExecuteVariable -> master.type
                is LangCompiler.ContextExecuteVariable -> typeMap[master.lastId]!!
                else -> throw LangCompileException("未知错误")
            }
            val otherType = when (other) {
                is LangCompiler.ContextExecuteVariable -> typeMap[other.lastId]
                    ?: throw LangCompileException("未知错误")
                is LangCompiler.DataTypeUpdater -> other.type
                is LangCompiler.ContextExecuteVariable -> typeMap[other.lastId]!!
                else -> throw LangCompileException("未知错误")
            }
            if (masterType != otherType) {
                throw LangCompileException("变量 $master 与 $other 类型不一致.")
            }
            val tryExecute = exp.execute.tryExecute(masterType)
            typeMap[id] = tryExecute
            returnType = tryExecute
        }
        for (v in copy) {
            if (v.left is LangCompiler.VariableExecuteVariable && v.right is LangCompiler.VariableExecuteVariable) {
                val left = variables[v.left.name] ?: throw LangCompileException("没有变量 ${v.left}.")
                val right = variables[v.right.name] ?: throw LangCompileException("没有变量 ${v.right}.")
                if (left != right) {
                    throw LangCompileException("变量 ${v.left.name}($left) 与 ${v.right.name}($right) 类型不一致.")
                }
                typeMap[v.id] = v.exp.execute.tryExecute(left)
            } else if (v.left is LangCompiler.VariableExecuteVariable) {
                v.loadOnesType(v.left, v.right)
            } else if (v.right is LangCompiler.VariableExecuteVariable) {
                v.loadOnesType(v.right, v.left)
            } else if (v.left is LangCompiler.ConstExecuteVariable && v.right is LangCompiler.ConstExecuteVariable) {
                if (v.left.type != v.right.type) {
                    throw LangCompileException("变量 ${v.right} 与 ${v.left} 类型不一致.")
                }
                typeMap[v.id] = v.exp.execute.tryExecute(v.left.type)
            } else {
                v.loadOnesType(v.right, v.left) // 上下文有关类型推断
            }
        }
        return typeMap[copy.last().id]!!
    }

    fun execute(variables: Map<String, Pair<LangDataType, String>>): Pair<LangDataType, String> {
        val typeMap = LinkedHashMap<Int, LangCompiler.ConstExecuteVariable>()
        var lastResult = LangCompiler.ConstExecuteVariable("", LangDataType.ANY)
        fun LangCompiler.IExecuteVariable.value(): LangCompiler.ConstExecuteVariable {
            return when (this) {
                is LangCompiler.ConstExecuteVariable -> {
                    return this
                }
                is LangCompiler.VariableExecuteVariable -> {
                    val data = variables[this.name] ?: throw LangCompileException("没有变量 ${this.name}.")
                    LangCompiler.ConstExecuteVariable(data.second, data.first)
                }
                is LangCompiler.ContextExecuteVariable -> {
                    typeMap[this.lastId] ?: throw LangCompileException("未知错误！无法拿到上下文 #${this.lastId} 数据.")
                }
                else -> throw LangCompileException("未知错误！")
            }
        }
        for (v in expressions) {
            val left = v.left.value()
            val right = v.right.value()
            if (left.type != right.type) {
                throw LangCompileException("表达式 \" $left ${v.exp} $right \" 左右类型不一致，.")
            }
            lastResult = v.exp.execute.execute(left.data, right.data, left.type)
            typeMap[v.id] = lastResult
        }
        return Pair(lastResult.type, lastResult.data)
    }
}
