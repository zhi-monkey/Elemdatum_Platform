package orm

func DeleteAll() {
	dataBase.Exec("DELETE FROM services")
	dataBase.Exec("DELETE FROM algorithm_monitors")
	dataBase.Exec("DELETE FROM monitors")
	dataBase.Exec("DELETE FROM algorithms")
}
