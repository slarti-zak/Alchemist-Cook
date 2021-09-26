package click.alchemist.cook.extension

import click.alchemist.cook.model.DatabaseObject
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

// Equals
infix fun String.equalTo(s: String?): Expression {
	return if (this == DatabaseObject::id.name) {
		Meta.id.equalTo(Expression.string(s))
	} else {
		Expression.property(this).equalTo(Expression.string(s))
	}
}

infix fun KProperty1<*, String>.equalTo(s: String?): Expression = this.name equalTo s

infix fun KProperty1<*, String>.equalTo(c: KClass<*>): Expression = this.name equalTo c.simpleName

infix fun String.equalTo(bool: Boolean): Expression = Expression.property(this).equalTo(Expression.booleanValue(bool))

infix fun KProperty1<*, Boolean>.equalTo(bool: Boolean): Expression = this.name equalTo bool

// In
infix fun KProperty1<*, String>.isIn(s: List<String>): Expression {
	val inExpressions = s.map { Expression.string(it) }.toTypedArray()
	return if (this.name == DatabaseObject::id.name) {
		Meta.id.`in`(*inExpressions)
	} else {
		Expression.property(this.name).`in`(*inExpressions)
	}
}