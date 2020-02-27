import java.util.*;

public class lab1 {
    public final static int machine_size = 300;
    public final static int arbitrary_limit = 8;
    public static String warning = "";
    public static String limit_warning = "";

    public static void main(String[] args) {
        // main method
        ArrayList<Module> modules = read_input();
        ArrayList<Symbol> table = new ArrayList<>();
        pass_one(modules, table);
        pass_two(modules, table);
    }

    public static ArrayList<Module> read_input() {
        // This class reads the input & returns Module array
        Scanner scanner = new Scanner(System.in);
        // # of modules
        int number_of_modules = scanner.nextInt();
        // Create module array
        ArrayList<Module> modules = new ArrayList<>();

        for (int i = 0; i < number_of_modules; i++) {
            Module m = new Module(); // Create new module obj

            int num = i; // keeps track of which module we're creating
            m.num = num;

            int ND = scanner.nextInt();
            m.ND = ND;

            int base_address;
            // If not first module, compute base address
            if (i != 0) {
                base_address = modules.get(i - 1).base_address + modules.get(i - 1).NT;
            } else {
                base_address = 0;
            }

            m.base_address = base_address;

            if (ND != 0) {
                // Create definitions array
                ArrayList<Symbol> definitions = new ArrayList<>();

                for (int j = 0; j < ND; j++) {
                    String symbol = scanner.next();
                    int symbol_location = scanner.nextInt();

                    Symbol def = new Symbol(num, symbol, symbol_location);
                    definitions.add(def);
                }

                m.definition_list = definitions;
            }

            int NU = scanner.nextInt();
            m.NU = NU;

            if (NU != 0) {
                // Create uses array
                ArrayList<Symbol> uses = new ArrayList<>();

                for (int j = 0; j < NU; j++) {
                    Symbol use = new Symbol();
                    use.num = num;

                    String symbol = scanner.next();

                    while (true) { // Infinite loop will run until -1 encounter
                        int use_location = scanner.nextInt();

                        if (use_location == -1)
                            break;

                        use.use_loc.add(use_location);
                    }

                    use.symbol = symbol;
                    uses.add(use);
                }

                m.use_list = uses;
            }

            int NT = scanner.nextInt();
            m.NT = NT;

            if (NT != 0) {
                ArrayList<Text> texts = new ArrayList<>();

                for (int j = 0; j < NT; j++) {
                    int word = scanner.nextInt(); // Full 5 digits
                    int opcode = word / 10000; // First digit
                    int address_with_type = word - (opcode * 10000); // Last 4 digits
                    int address = address_with_type / 10;
                    int type = word % 10; // last digit

                    Text w = new Text(num, opcode, address_with_type, address, type);

                    texts.add(w);
                }

                m.texts = texts;
            }

            modules.add(m);
        }

        scanner.close();
        return modules;
    }

    public static void pass_one(ArrayList<Module> modules, ArrayList<Symbol> table) {
        // Make & print symbol table
        for (Module module : modules) {
            if (module.ND != 0) {
                // module has definitions
                for (Symbol definition : module.definition_list) {
                    // Check if already defined
                    Symbol duplicate = locate_duplicate(definition, table);

                    if (duplicate == null) {
                        // Definition is not defined
                        // Add to table
                        table.add(definition);
                    } else {
                        // Definition is already defined
                        // Update error & use last value
                        duplicate.error += "ERROR: Variable multiply defined. Using most recent value";
                        // Update num
                        duplicate.num = definition.num;
                        // Update relative location to recent
                        duplicate.relative_loc = definition.relative_loc;
                    }

                    if (definition.relative_loc >= module.NT) {
                        // Definition address is greater than module size; use legal max
                        definition.error += "ERROR: Definition exceeds module size; last word in module used.";
                        definition.relative_loc = module.NT - 1;
                    }
                }
            }
        }

        for (Symbol table_content : table) {
            // Update absolute loc
            table_content.absolute_loc = modules.get(table_content.num).base_address + table_content.relative_loc;
        }

    }

    public static void pass_two(ArrayList<Module> modules, ArrayList<Symbol> table) {
        for (Module module : modules) {
            for (Text text : module.texts) {
                // Check for error
                switch (text.address_type) {
                case 2:
                    // Absolute
                    // If address (middle 3 digits) exceeds machine_size, use max size
                    if (text.address > machine_size - 1) {
                        text.error += "ERROR: A text address exceeds the machine size of 300. Using 299";
                        text.address = machine_size - 1;
                    }
                    break;
                case 3:
                    // Relative
                    // If relative address exceeds module size, then error
                    if (text.address >= module.NT) {
                        text.error += "ERROR: A relative text address exceeds the module size. Using max legal size.";
                        text.address = module.NT - 1;
                    } else {
                        text.address = module.base_address + text.address;
                    }
                    break;
                default:
                    break;
                }
            }
        }

        // Resolve external references & error handling
        for (Module module : modules) {
            if (module.use_list != null) {
                for (Symbol use : module.use_list) {
                    Symbol duplicate = locate_duplicate(use, table);

                    if (duplicate != null) {
                        // Symbol is in definitions

                        for (int use_loc : use.use_loc) {
                            if (module.texts.get(use_loc).used_symbol != null) {
                                // If multiple symbols used for one text, error
                                module.texts.get(
                                        use_loc).error += "ERROR: Multiple variables used in instruction; all but last ignored.";
                                // Set used_symbol of Text as most recent
                                module.texts.get(use_loc).used_symbol = use;
                            } else {
                                // Add symbol to used_symbol
                                module.texts.get(use_loc).used_symbol = use;
                            }
                        }
                    } else {
                        // Symbol is not defined, save error & use 111
                        ArrayList<Integer> use_loc = use.use_loc;
                        for (int j : use_loc) {
                            module.texts.get(j).error += "ERROR: " + use.symbol + " used but not defined. 111 used.";
                            // Special case: Symbol is not defined but used, and Symbol is also longer than
                            // arbitrary length
                            if (!limit_warning.contains(use.symbol)) {
                                // Only store if it's first time
                                limit_warning += "WARNING: Symbol " + use.symbol
                                        + " is longer than the arbitrary limit of " + arbitrary_limit + ".\n";
                            }
                        }
                    }
                }
            }
        }

        // External reference resolve & Error handling
        for (Module module : modules) {
            for (Text text : module.texts) {
                if (text.address_type == 4) {
                    // External
                    if (text.error.contains("111")) {
                        // Use 111 as address
                        text.address = 111;
                    } else {
                        // Set address (middle 3 digits) as absolute address of symbol
                        text.address = locate_duplicate(text.used_symbol, table).absolute_loc;
                    }
                }
            }
        }

        // If symbol was defined but never used, update warning and continue
        if (table != null) {
            for (Symbol table_content : table) {
                for (Module module : modules) {
                    if (module.use_list != null) {
                        Symbol duplicate = locate_duplicate(table_content, module.use_list);
                        table_content.used = (duplicate != null) ? true : false;
                        if (table_content.used == true)
                            break;
                    }
                }
            }

            for (Symbol table_content : table) {
                if (table_content.used == false) {
                    // Symbol defined but never used
                    warning += "WARNING: The symbol: " + table_content.symbol + " was defined in module "
                            + table_content.num + " but never used.\n";
                }

                if (table_content.symbol.length() > arbitrary_limit) {
                    // Symbol exceeds arbitrary length
                    limit_warning += "WARNING: Symbol " + table_content.symbol
                            + " is longer than the arbitrary limit of " + arbitrary_limit + ".\n";
                }
            }
        }

        // Print symbol table
        print_table(table);
        // Print Memory Map
        print_memory(modules);
        // Print Warnings
        if (warning.length() != 0)
            System.out.println(warning);
        // Print Warning of arbitrary limits
        if (limit_warning.length() != 0)
            System.out.println(limit_warning);
    }

    public static void print_memory(ArrayList<Module> modules) {
        int i = 0;
        System.out.println("Memory Map");
        for (Module module : modules) {
            if (module.texts != null) {
                for (Text text : module.texts) {
                    System.out.println(i + ": " + text.opcode + String.format("%03d ", text.address) + text.error);
                    i++;
                }
            }
        }
        System.out.println(); // newline after table
    }

    public static void print_table(ArrayList<Symbol> table) {
        System.out.println("Symbol Table");
        for (Symbol def : table) {
            System.out.println(def.symbol + "=" + def.absolute_loc + " " + def.error);
        }
        System.out.println(); // newline after table
    }

    public static Symbol locate_duplicate(Symbol definition, ArrayList<Symbol> definition_list) {
        for (Symbol def : definition_list) {
            if (definition.symbol.equals(def.symbol)) {
                return def;
            }
        }
        return null;
    }
}

class Module {
    public int num; // keeps track of which module we're in
    public int base_address;
    public int ND; // # definitions
    public int NU; // # use lists
    public int NT; // # length of module & words
    ArrayList<Symbol> definition_list;
    ArrayList<Symbol> use_list;
    ArrayList<Text> texts;

    // Empty constructor
    public Module() {
    }
}

class Symbol {
    public int num;
    public String symbol;
    public int relative_loc;
    public int absolute_loc;
    public ArrayList<Integer> use_loc = new ArrayList<>();
    public String error = "";
    public boolean used;

    // Constructor for definitions
    public Symbol(int num, String symbol, int relative_loc) {
        this.num = num;
        this.symbol = symbol;
        this.relative_loc = relative_loc;
    }

    // Empty constructor
    public Symbol() {
    }
}

class Text {
    public int num;
    public int opcode;
    public int address_with_type;
    public int address;
    public int address_type;
    public Symbol used_symbol;
    public String error = "";

    // Constructor
    public Text(int num, int opcode, int address_with_type, int address, int address_type) {
        this.num = num;
        this.opcode = opcode;
        this.address_with_type = address_with_type;
        this.address = address;
        this.address_type = address_type;
    }
}
