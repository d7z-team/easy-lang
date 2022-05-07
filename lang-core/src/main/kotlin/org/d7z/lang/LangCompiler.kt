package org.d7z.lang

import java.util.LinkedList

class LangCompiler(
    role: String,
) {
    /**
     * 执行算法
     */
    private val expressions: List<ExecuteExpression>

    private val syntaxTree: RuleTree

    init {
        val grammars = getGrammar(role) // 语法分析
        semanticCheck(grammars) // 语法检查
        syntaxTree = loadSyntaxTree(grammars) // 构建语法树
        expressions = loadInstruction(syntaxTree) // 构建四元表达式
        while (true) { // 优化算法
            if (optimization(expressions)) {
                break
            }
        }
    }

    /**
     * 优化算法
     */
    private fun optimization(expressions: MutableList<ExecuteExpression>): Boolean {
        val map = hashMapOf<Int, ConstExecuteVariable>()
        expressions.forEach {
            if (it.left is ConstExecuteVariable && it.right is ConstExecuteVariable) {
                if (it.left.type != it.right.type) {
                    throw LangCompileException("语法错误：$it 左右类型不一致.")
                }
                it.returnType = it.exp.execute.tryExecute(it.left.type)
                map[it.id] = it.exp.execute.execute(it.left.data, it.right.data, it.left.type)
            }
        }
        val copy = expressions.map { it }.toList()
        expressions.removeIf { it.id in map.keys }
        if (expressions.isEmpty()) {
            expressions.addAll(copy)
            return true
        }
        for ((index, data) in expressions.withIndex()) {
            if (data.left is ContextExecuteVariable && data.left.lastId in map.keys &&
                data.right is ContextExecuteVariable && data.right.lastId in map.keys
            ) {
                expressions[index] = ExecuteExpression(
                    id = data.id,
                    left = map[data.left.lastId]!!,
                    right = map[data.right.lastId]!!,
                    exp = data.exp
                )
            } else if (data.left is ContextExecuteVariable && data.left.lastId in map.keys) {
                expressions[index] = ExecuteExpression(
                    id = data.id,
                    left = map[data.left.lastId]!!,
                    right = data.right,
                    exp = data.exp
                )
            } else if (data.right is ContextExecuteVariable && data.right.lastId in map.keys) {
                expressions[index] = ExecuteExpression(
                    id = data.id,
                    left = data.left,
                    right = map[data.right.lastId]!!,
                    exp = data.exp
                )
            }
        }
        if (map.isNotEmpty()) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return """
            tree: $syntaxTree
            build:
            ${expressions.joinToString("\r\n")}
        """.lines().joinToString("\r\n") { it.trim() }.trim()
    }

    /**
     * 四元式
     */
    data class ExecuteExpression(
        val id: Int,
        val left: IExecuteVariable,
        val right: IExecuteVariable,
        val exp: LangOperator
    ) {
        var returnType: LangDataType = LangDataType.ANY
        override fun toString() =
            "#${String.format("%02d", id)}: ${format(left)} ${
            String.format(
                "%-5s",
                exp.opera[0]
            )
            } ${format(right)}   = ${
            String.format(
                "%-15s",
                "$returnType(?)"
            )
            }"

        private fun format(data: IExecuteVariable): String {
            return when (data) {
                is VariableExecuteVariable -> "${data.type}(\$${data.name})"
                is ConstExecuteVariable -> "${data.type}('${data.data}')"
                is ContextExecuteVariable -> "#${String.format("%02d", data.lastId)}"
                else -> throw LangCompileException("未知错误！")
            }.let {
                String.format("%-15s", it)
            }
        }
    }

    interface IExecuteVariable
    data class ConstExecuteVariable(val data: String, override var type: LangDataType) :
        IExecuteVariable,
        DataTypeUpdater {
        override fun toString() = "$type('$data')"
    }

    data class VariableExecuteVariable(val name: String, override var type: LangDataType) :
        IExecuteVariable,
        DataTypeUpdater {
        override fun toString() = "$type(\$$name)"
    }

    data class ContextExecuteVariable(val lastId: Int) : IExecuteVariable

    interface DataTypeUpdater {
        var type: LangDataType
    }

    fun build(): LangRunner {
        return LangRunner(this.expressions)
    }

    /**
     *  计算执行指令
     */
    private fun loadInstruction(syntaxTree: RuleTree): MutableList<ExecuteExpression> {
        var id = 0

        val linkedList: LinkedList<ExecuteExpression> = LinkedList()
        fun internalExecute(syntaxTree: RuleTree): Int {
            fun IChildTree.format(): IExecuteVariable {
                return when (this) {
                    is ConstChildTree -> ConstExecuteVariable(data, type)
                    is VariableChildTree -> VariableExecuteVariable(name, type)
                    else -> throw LangCompileException("未知错误！")
                }
            }
            if (syntaxTree.right is EmptyChildTree) {
                linkedList.addFirst(
                    ExecuteExpression(
                        ++id, syntaxTree.left.format(), syntaxTree.left.format(), LangOperator.COPY_LEFT
                    )
                )
                return id
            } else if (syntaxTree.left is EmptyChildTree) {
                linkedList.addFirst(
                    ExecuteExpression(
                        ++id, syntaxTree.right.format(), syntaxTree.right.format(), LangOperator.COPY_LEFT
                    )
                )
                return id
            } else if (syntaxTree.left !is FunChildTree && syntaxTree.right !is FunChildTree) {
                linkedList.addFirst(
                    ExecuteExpression(
                        ++id, syntaxTree.left.format(), syntaxTree.right.format(), syntaxTree.expr
                    )
                )
                return id
            } else if (syntaxTree.left is FunChildTree && syntaxTree.right is FunChildTree) {
                val id1 = internalExecute(syntaxTree.left.child)
                val id2 = internalExecute(syntaxTree.right.child)
                linkedList.addLast(
                    ExecuteExpression(
                        ++id, ContextExecuteVariable(id1), ContextExecuteVariable(id2), syntaxTree.expr
                    )
                )
                return id
            } else if (syntaxTree.left is FunChildTree) {
                val id1 = internalExecute(syntaxTree.left.child)
                linkedList.addLast(
                    ExecuteExpression(
                        ++id, ContextExecuteVariable(id1), syntaxTree.right.format(), syntaxTree.expr
                    )
                )
                return id
            } else if (syntaxTree.right is FunChildTree) {
                val id2 = internalExecute(syntaxTree.right.child)
                linkedList.addLast(
                    ExecuteExpression(
                        ++id, syntaxTree.left.format(), ContextExecuteVariable(id2), syntaxTree.expr
                    )
                )
                return id
            } else {
                throw LangCompileException("未知错误")
            }
        }
        internalExecute(syntaxTree)
        linkedList.sortBy { it.id }
        return linkedList
    }

    /**
     * 语法树
     */
    data class RuleTree(
        val left: IChildTree,
        val expr: LangOperator,
        val right: IChildTree,
    ) {
        override fun toString() = "$left ${expr.opera[0]} $right"
    }

    /**
     * 语法树标记
     */
    interface IChildTree
    data class ConstChildTree(val data: String, var type: LangDataType) : IChildTree {
        override fun toString() = "$type('$data')"
    }

    data class VariableChildTree(val name: String, var type: LangDataType) : IChildTree {
        override fun toString() = "$type(\$$name)"
    }

    data class FunChildTree(val child: RuleTree) : IChildTree {
        override fun toString() = "( $child )"
    }

    class EmptyChildTree : IChildTree

    private fun <D : Grammar> List<D>.covert(): IChildTree {
        return if (size == 1) {
            when (val data = first()) {
                is VarGrammar -> {
                    VariableChildTree(data.name, data.type)
                }
                is ConstGrammar -> {
                    ConstChildTree(data.data, data.type)
                }
                is FuncGrammar -> {
                    val tree = loadSyntaxTree(data.child) // 优化嵌套
                    if (tree.left is EmptyChildTree && tree.right !is EmptyChildTree) {
                        tree.right
                    } else if (tree.right is EmptyChildTree && tree.left !is EmptyChildTree) {
                        tree.left
                    } else {
                        FunChildTree(tree)
                    }
                }
                else -> throw LangCompileException("未知错误")
            }
        } else {
            val childTree = loadSyntaxTree(this) // 优化嵌套
            if (childTree.left is EmptyChildTree && childTree.right !is EmptyChildTree) {
                childTree.right
            } else if (childTree.right is EmptyChildTree && childTree.left !is EmptyChildTree) {
                childTree.left
            } else {
                FunChildTree(childTree)
            }
        }
    }

    private fun loadSyntaxTree(grammars: List<Grammar>): RuleTree {
        if (grammars.size == 1) {
            return RuleTree(grammars.covert(), LangOperator.ADD, EmptyChildTree())
        }
        val data = operators.firstNotNullOf { exp ->
            grammars.withIndex().firstOrNull {
                it.value is ExpGrammar && (it.value as ExpGrammar).data == exp
            }
        }
        val leftList = grammars.subList(0, data.index).covert()
        val rightList = grammars.subList(data.index + 1, grammars.size).covert()
        return RuleTree(leftList, operatorMap[(data.value as ExpGrammar).data]!!, rightList)
    }

    /**
     * 语法检查器
     */
    private fun semanticCheck(grammars: List<Grammar>) {
        for (pairs in grammars.windowed(2, 2, false)) { // 语法检查
            if (pairs.first()::class !in arrayOf(VarGrammar::class, ConstGrammar::class, FuncGrammar::class)) {
                throw LangCompileException("${pairs.first()}  应为变量或函数体.")
            }
            if (pairs.last()::class != ExpGrammar::class) {
                throw LangCompileException("${pairs.last()}  应为运算符.")
            }
        }
        if (grammars.last()::class !in arrayOf(VarGrammar::class, ConstGrammar::class, FuncGrammar::class)) {
            throw LangCompileException("${grammars.last()}  应为变量或函数体.")
        }
        grammars.filterIsInstance<VarGrammar>().forEach { // 变量命名检查
            if (it.name.contains(Regex("^[a-zA-Z][_\\\\.a-zA-Z\\d]+\$")).not()) {
                throw LangCompileException("变量 $it 不符合命名要求.")
            }
        }
        grammars.filterIsInstance<FuncGrammar>().forEach { // 子函数体检查
            semanticCheck(it.child)
        }
    }

    /**
     * 原语标记
     */
    interface Grammar

    data class VarGrammar(val name: String, var type: LangDataType = LangDataType.ANY) : Grammar
    data class ConstGrammar(val data: String, val type: LangDataType) : Grammar
    data class ExpGrammar(val data: String) : Grammar
    data class FuncGrammar(val child: List<Grammar>) : Grammar

    /**
     * 获取解析后的语法
     */
    private fun getGrammar(role: String): List<Grammar> {
        fun findNextOperator(data: String, offset: Int): IndexedValue<String> {
            var index = offset
            while (index < data.length) {
                val current = data[index]
                for (operator in operators) { // 运算符查找
                    if (operator == current.toString()) {
                        return IndexedValue(index, operator)
                    }
                    if (operator.startsWith(current)) {
                        val opera = data.substring(index, (index + operator.length).coerceAtMost(data.length))
                        if (opera == operator) {
                            return IndexedValue(index, opera)
                        }
                    }
                }
                index++
            }
            return IndexedValue(-1, "")
        }

        val grammar = LinkedList<Grammar>()
        if (role.isBlank()) {
            throw LangCompileException("表达式为空，无法解析")
        }
        var pointer = 0
        while (pointer < role.length) {
            val current = role[pointer]
            if (current == '\'') { // 常量查找
                val last = role.indexOf("\'", pointer + 1, false)
                if (last == -1) {
                    throw LangCompileException("语法错误，无法找到常量结束符:${role.substring(pointer)}")
                }
                grammar.add(ConstGrammar(role.substring(pointer + 1, last), LangDataType.TEXT))
                pointer = last + 1
            }
            if (current == '(') { // 函数查找
                val last = role.substring(pointer + 1).let {
                    var leftC = 0
                    for (c in it.withIndex()) {
                        if (c.value == '(') {
                            leftC++
                        }
                        if (c.value == ')') {
                            if (leftC == 0) {
                                return@let c.index + pointer + 1
                            }
                            leftC--
                        }
                    }
                    throw LangCompileException("语法错误，无法找到结束符')' :${role.substring(pointer)}")
                }
                if (last == -1) {
                    throw LangCompileException("语法错误，无法找到结束符')' :${role.substring(pointer)}")
                }
                grammar.add(FuncGrammar(getGrammar(role.substring(pointer + 1, last).trim())))
                pointer = last + 1
            }
            if (current in arrayOf(' ', '\r', '\n')) {
                pointer += 1
                continue
            }
            val nextOperator = findNextOperator(role, pointer)
            when (nextOperator.index) {
                -1 -> {
                    val variable = role.substring(pointer).trim()
                    if (variable.isNotBlank()) {
                        variable.toDoubleOrNull()?.let {
                            grammar.add(ConstGrammar(variable, LangDataType.NUMBER))
                        } ?: variable.toBooleanStrictOrNull()?.let {
                            grammar.add(ConstGrammar(variable, LangDataType.BOOL))
                        } ?: grammar.add(VarGrammar(variable, LangDataType.ANY))
                    }
                    break
                }
                else -> {
                    val variable = role.substring(pointer, nextOperator.index).trim()
                    if (variable.isNotBlank()) {
                        variable.toDoubleOrNull()?.let {
                            grammar.add(ConstGrammar(variable, LangDataType.NUMBER))
                        } ?: variable.toBooleanStrictOrNull()?.let {
                            grammar.add(ConstGrammar(variable, LangDataType.BOOL))
                        } ?: grammar.add(VarGrammar(variable, LangDataType.ANY))
                    }
                    grammar.add(ExpGrammar(nextOperator.value))
                    pointer = nextOperator.index + nextOperator.value.length
                }
            }
        }
        return grammar
    }

    companion object {

        private val operatorMap = LangOperator.values().flatMap { it.opera.map { name -> name to it } }.toMap()
        private val operators = LangOperator.values().flatMap { it.opera.toList() }.toList()
    }
}
