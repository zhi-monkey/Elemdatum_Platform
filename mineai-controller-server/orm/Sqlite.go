package orm

import (
	"github.com/glebarez/sqlite"
	"gorm.io/gorm"
	"mineai-controller-server/orm/entity"
)

var dataBase *gorm.DB

func SqliteConnect() {
	db, err := gorm.Open(sqlite.Open("config.db"), &gorm.Config{})
	if err != nil {
		panic(any("failed to connect database"))
	}
	// 启用 WAL 模式
	_ = db.Exec("PRAGMA journal_mode=WAL;")
	_ = db.Exec("PRAGMA sqlite_threadsafe = 2;")
	_ = db.Exec("PRAGMA busy_timeout=5000;")
	err = db.SetupJoinTable(&entity.Algorithm{}, "Monitors", &entity.AlgorithmMonitor{})
	if err != nil {
		panic(any("数据库关系迁移失败"))
	}
	err = db.AutoMigrate(&entity.CenterServer{}, &entity.Service{}, &entity.Algorithm{}, &entity.Monitor{}, &entity.ModelAlert{})
	if err != nil {
		panic(any("数据库关系迁移失败"))
	}
	dataBase = db
	sqlDB, _ := db.DB()
	sqlDB.SetMaxOpenConns(100)
	sqlDB.SetMaxIdleConns(20)
}
