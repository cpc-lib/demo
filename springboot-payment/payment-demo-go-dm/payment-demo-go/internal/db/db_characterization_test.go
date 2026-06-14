package db

import (
	"testing"

	"payment-demo-go/internal/config"
)

func TestDialector_CurrentBehaviorUsesDameng(t *testing.T) {
	dialector := Dialector(config.DatabaseConfig{
		Driver: "dm",
		DSN:    "dm://SYSDBA:SYSDBA@127.0.0.1:5236?schema=PAYMENT_DEMO",
	})

	if dialector.Name() != "dm" {
		t.Fatalf("Dialector().Name() = %q, want dm", dialector.Name())
	}
}
