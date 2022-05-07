package org.d7z.lang

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LangOperator(val opera: Array<String>, val types: Array<LangDataType>, val execute: Execute) {
    AND(arrayOf("&", " and ", " AND "), arrayOf(LangDataType.BOOL), OrAndExecute(false)),
    OR(arrayOf("|", " or ", " OR "), arrayOf(LangDataType.BOOL), OrAndExecute(true)),
    GREATER_EQ(arrayOf(">="), arrayOf(LangDataType.NUMBER, LangDataType.TIME), CompareExecute(">=")),
    LESS_EQ(arrayOf("<="), arrayOf(LangDataType.NUMBER, LangDataType.TIME), CompareExecute("<=")),
    EQ(arrayOf("="), LangDataType.values(), CompareExecute("=")),
    GREATER(arrayOf(">"), arrayOf(LangDataType.NUMBER, LangDataType.TIME), CompareExecute(">")),
    LESS(arrayOf("<"), arrayOf(LangDataType.NUMBER, LangDataType.TIME), CompareExecute("<")),
    ADD(arrayOf("+"), arrayOf(LangDataType.NUMBER, LangDataType.TIME, LangDataType.TEXT), CalculateExecute("+")),
    MINUS(arrayOf("-"), arrayOf(LangDataType.NUMBER), CalculateExecute("-")),
    TIMES(arrayOf("*"), arrayOf(LangDataType.NUMBER, LangDataType.TIME), CalculateExecute("*")),
    DIV(arrayOf("/"), arrayOf(LangDataType.NUMBER), CalculateExecute("/")),
    REM(arrayOf("%"), arrayOf(LangDataType.NUMBER), CalculateExecute("%")),
    COPY_LEFT(emptyArray(), LangDataType.values(), CopyLeft());

    interface Execute {
        /**
         * 尝试执行获取返回对象类型
         */
        fun tryExecute(dataType: LangDataType): LangDataType

        /**
         * 执行并返回结果
         */
        fun execute(first: String, second: String, type: LangDataType): LangCompiler.ConstExecuteVariable
    }

    class CopyLeft : Execute {
        override fun tryExecute(dataType: LangDataType): LangDataType {
            return dataType
        }

        override fun execute(first: String, second: String, type: LangDataType): LangCompiler.ConstExecuteVariable {
            return LangCompiler.ConstExecuteVariable(first, type)
        }
    }

    companion object {
        val dataFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    }

    class OrAndExecute(private val isOr: Boolean) : Execute {
        override fun tryExecute(dataType: LangDataType): LangDataType {
            if (dataType != LangDataType.BOOL) {
                throw LangCompileException("不支持 $dataType 类型.")
            }
            return LangDataType.BOOL
        }

        override fun execute(first: String, second: String, type: LangDataType): LangCompiler.ConstExecuteVariable {
            return if (isOr) {
                LangCompiler.ConstExecuteVariable(
                    (first.toBoolean() || second.toBoolean()).toString(),
                    LangDataType.BOOL
                )
            } else {
                LangCompiler.ConstExecuteVariable(
                    (first.toBoolean() && second.toBoolean()).toString(),
                    LangDataType.BOOL
                )
            }
        }
    }

    class CompareExecute(private val func: String) : Execute {
        override fun tryExecute(dataType: LangDataType): LangDataType {
            if (func in arrayOf(">=", ">", "<=", "<") && dataType !in arrayOf(
                    LangDataType.NUMBER,
                    LangDataType.TIME,
                    EQ
                )
            ) {
                throw LangCompileException("表达式 $func 不支持 $dataType 类型.")
            }
            return LangDataType.BOOL
        }

        override fun execute(first: String, second: String, type: LangDataType): LangCompiler.ConstExecuteVariable {
            when (type) {
                LangDataType.NUMBER -> {
                    val firstDec = BigDecimal(first)
                    val secondDec = BigDecimal(second)
                    return when (func) {
                        ">=" -> firstDec.compareTo(secondDec) in arrayOf(0, 1)
                        ">" -> firstDec.compareTo(secondDec) == 1
                        "<=" -> firstDec.compareTo(secondDec) in arrayOf(0, -1)
                        "<" -> firstDec.compareTo(secondDec) == -1
                        "=" -> firstDec.compareTo(secondDec) == 0
                        else -> throw LangCompileException("不支持表达式 $func.")
                    }.let { LangCompiler.ConstExecuteVariable(it.toString(), LangDataType.BOOL) }
                }
                LangDataType.TIME -> {
                    val firstTime = LocalDateTime.parse(first, dataFormat)
                    val secondTime = LocalDateTime.parse(second, dataFormat)
                    return when (func) {
                        ">=" -> firstTime.isAfter(secondTime) && firstTime.isEqual(secondTime)
                        ">" -> firstTime.isAfter(secondTime)
                        "<=" -> firstTime.isBefore(secondTime) && firstTime.isEqual(secondTime)
                        "<" -> firstTime.isBefore(secondTime)
                        "=" -> firstTime.isEqual(secondTime)
                        else -> throw LangCompileException("不支持表达式 $func.")
                    }.let { LangCompiler.ConstExecuteVariable(it.toString(), LangDataType.BOOL) }
                }
                else -> {
                    if (func == "=") {
                        return LangCompiler.ConstExecuteVariable((first == second).toString(), LangDataType.BOOL)
                    } else {
                        throw LangCompileException("类型 $type 不支持表达式 $func.")
                    }
                }
            }
        }
    }

    class CalculateExecute(private val func: String) : Execute {
        override fun tryExecute(dataType: LangDataType): LangDataType {
            if (func == "+" && dataType !in arrayOf(LangDataType.NUMBER, LangDataType.TEXT) ||
                func == "-" && dataType !in arrayOf(LangDataType.NUMBER, LangDataType.TIME, LangDataType.TIME) ||
                func in arrayOf("*", "/", "%") && dataType !in arrayOf(LangDataType.NUMBER)
            ) {
                throw LangCompileException("表达式 $func 不支持 $dataType 类型.")
            }
            if (dataType == LangDataType.TIME) {
                return LangDataType.NUMBER
            }
            return dataType
        }

        override fun execute(first: String, second: String, type: LangDataType): LangCompiler.ConstExecuteVariable {
            return when (func) {
                "+" -> {
                    when (type) {
                        LangDataType.NUMBER -> BigDecimal(first).plus(BigDecimal(second)).toPlainString()
                        LangDataType.TEXT -> first + second
                        else -> throw LangCompileException("表达式 $func 不支持 $type 类型.")
                    }.let { LangCompiler.ConstExecuteVariable(it, type) }
                }
                "-" -> {
                    when (type) {
                        LangDataType.NUMBER -> LangCompiler.ConstExecuteVariable(
                            BigDecimal(first).minus(BigDecimal(second)).toPlainString(), LangDataType.NUMBER
                        )
                        LangDataType.TEXT -> LangCompiler.ConstExecuteVariable(first + second, LangDataType.TEXT)
                        LangDataType.TIME -> LangCompiler.ConstExecuteVariable(
                            Duration.between(
                                LocalDateTime.parse(first, dataFormat),
                                LocalDateTime.parse(second, dataFormat)
                            ).toSeconds().toString(),
                            LangDataType.NUMBER
                        )
                        else -> throw LangCompileException("表达式 $func 不支持 $type 类型.")
                    }
                }
                "*", "/", "%" -> {
                    if (type == LangDataType.NUMBER) {
                        when (func) {
                            "*" -> BigDecimal(first).times(BigDecimal(second)).toPlainString()
                            "/" -> BigDecimal(first).div(BigDecimal(second)).toPlainString()
                            "%" -> BigDecimal(first).rem(BigDecimal(second)).toPlainString()
                            else -> throw LangCompileException("表达式 $func 不支持 $type 类型.")
                        }.let { LangCompiler.ConstExecuteVariable(it, LangDataType.NUMBER) }
                    } else {
                        throw LangCompileException("表达式 $func 不支持 $type 类型.")
                    }
                }
                else -> throw LangCompileException("表达式 $func 不支持 $type 类型.")
            }
        }
    }
}
