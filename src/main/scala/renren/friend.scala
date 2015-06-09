package renren

class Friend(var uid: String, var school: String, var name: String, var link: String) extends Serializable {
	override def toString(): String =  {
		s"Friend($uid, $school, $name, $link)"
	}

	override def equals(o: Any) = o match {
		case that: Friend => that.uid.equalsIgnoreCase(this.uid)
		case _ => false
	}

	override def hashCode = uid.toUpperCase.hashCode

	// Friend($uid, $school, $name, $link)
	def parse(s: String) = {
		var content = s.substring(7, s.size - 2)
		var parameterList = content.split(", ")
		uid = parameterList(0)
		school = parameterList(1)
		name = parameterList(2)
		link = parameterList(3)
	}
	
}