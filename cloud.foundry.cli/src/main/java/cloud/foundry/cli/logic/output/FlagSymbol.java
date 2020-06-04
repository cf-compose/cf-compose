package cloud.foundry.cli.logic.output;

public enum FlagSymbol {
    ADDED("+"),
    REMOVED("-"),
    NONE(" ");

    private String symbol;

    private FlagSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
