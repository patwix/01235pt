package analyzer.visitors;

import analyzer.ast.*;
//import com.sun.javafx.geom.Edge;
import com.sun.org.apache.bcel.internal.generic.RET;
import com.sun.org.apache.xpath.internal.operations.Bool;

import javax.crypto.Mac;
import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    private Integer REG = 256; // default register limitation
    private ArrayList<String> RETURNED = new ArrayList<>(); // returned variables from the return statement
    private ArrayList<MachLine> CODE = new ArrayList<>(); // representation of the Machine Code in Machine lines (MachLine)
    private ArrayList<String> LOADED = new ArrayList<>(); // could be use to keep which variable/pointer are loaded/ defined while going through the intermediate code
    private ArrayList<String> MODIFIED = new ArrayList<>(); // could be use to keep which variable/pointer are modified while going through the intermediate code

    private HashMap<String, String> OP; // map to get the operation name from it's value

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OP = new HashMap<>();
        OP.put("+", "ADD");
        OP.put("-", "MIN");
        OP.put("*", "MUL");
        OP.put("/", "DIV");
    }

    private void LDIfNotInMemory(String var) {
        if (!LOADED.contains(var) && RETURNED.contains(var)) {

            // add the variable we're loading in the LOADED list
            LOADED.add(var);

            // create the line parameters array
            ArrayList<String> line = new ArrayList<>();
            line.add("LD");
            line.add(var);
            line.add(var.substring(1, 2));

            MachLine loadLine = new MachLine(line);

            // add the load line to machine instructions
            CODE.add(loadLine);
        }
    }

    private void STifChanged(String var) {
        //TODO
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {

        node.jjtGetChild(0).jjtAccept(this, data);
        node.jjtGetChild(2).jjtAccept(this, data);
        node.jjtGetChild(1).jjtAccept(this, data);

        compute_LifeVar(); // first Life variables computation (should be recalled when machine code generation)
        compute_NextUse(); // first Next-Use computation (should be recalled when machine code generation)
        compute_machineCode(); // generate the machine code from the CODE array (the CODE array should be transformed

        for (int i = 0; i < CODE.size(); i++) // print the output
            m_writer.println(CODE.get(i));
        return null;
    }


    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        REG = ((ASTIntValue) node.jjtGetChild(0)).getValue(); // get the limitation of register
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNED.add("@" + ((ASTIdentifier) node.jjtGetChild(i)).getValue()); // returned values (here are saved in "@*somthing*" format, you can change that if you want.

            // TODO: the returned variables should be added to the Life_OUT set of the last statement of the basic block (before the "ST" expressions in the machine code)
        }
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        LDIfNotInMemory(left); // check if variable is loaded and add it if not

        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);
        LDIfNotInMemory(right); // check if variable is loaded and add it if not

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = left op right" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")

        ArrayList<String> line = new ArrayList<>();
        line.add(OP.get(node.getOp()));
        line.add(assigned);
        line.add(left);
        line.add(right);

        MachLine assignStatementMachLine = new MachLine(line);
        this.CODE.add(assignStatementMachLine);


        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        LDIfNotInMemory(left); // check if variable is loaded and add it if not

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = - left" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")
        ArrayList<String> line = new ArrayList<>();
        line.add("SUB");
        line.add(assigned);
        line.add("#0");
        line.add(left);

        MachLine assignStatementMachLine = new MachLine(line);
        this.CODE.add(assignStatementMachLine);


        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        LDIfNotInMemory(left); // check if variable is loaded and add it if not

        // TODO: Modify CODE to add the needed MachLine.
        //       here the type of Assignment is "assigned = left" and you should put pointers in the MachLine at
        //       the moment (ex: "@a")
        ArrayList<String> line = new ArrayList<>();
        line.add("ADD");
        line.add(assigned);
        line.add("#0");
        line.add(left);

        MachLine assignStatementMachLine = new MachLine(line);
        this.CODE.add(assignStatementMachLine);

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        //nothing to do here
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        //nothing to do here
        return "#" + String.valueOf(node.getValue());
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        //nothing to do here
        return "@" + node.getValue();
    }

    private class NextUse {
        // NextUse class implementation: you can use it or redo it your way
        public HashMap<String, ArrayList<Integer>> nextuse = new HashMap<String, ArrayList<Integer>>();

        public NextUse() {
        }

        public NextUse(HashMap<String, ArrayList<Integer>> nextuse) {
            this.nextuse = nextuse;
        }

        public void add(String s, int i) {
            if (!nextuse.containsKey(s)) {
                nextuse.put(s, new ArrayList<Integer>());
            }
            nextuse.get(s).add(i);
        }

        public String toString() {
            String buff = "";
            boolean first = true;
            for (String k : set_ordered(nextuse.keySet())) {
                if (!first) {
                    buff += ", ";
                }
                buff += k + ":" + nextuse.get(k);
                first = false;
            }
            return buff;
        }

        public Object clone() {
            return new NextUse((HashMap<String, ArrayList<Integer>>) nextuse.clone());
        }
    }

    private class MachLine {
        List<String> line;
        public HashSet<String> REF = new HashSet<>();
        public HashSet<String> DEF = new HashSet<>();
        public HashSet<Integer> SUCC = new HashSet<>();
        public HashSet<Integer> PRED = new HashSet<>();
        public HashSet<String> Life_IN = new HashSet<>();
        public HashSet<String> Life_OUT = new HashSet<>();

        public NextUse Next_IN = new NextUse();
        public NextUse Next_OUT = new NextUse();

        public MachLine(List<String> s) {
            this.line = s;
            int size = CODE.size();

            // PRED, SUCC, REF, DEF already computed (cadeau )
            if (size > 0) {
                PRED.add(size - 1);
                CODE.get(size - 1).SUCC.add(size);
            }
            this.DEF.add(s.get(1));
            for (int i = 2; i < s.size(); i++)
                if (s.get(i).charAt(0) == '@')
                    this.REF.add(s.get(i));
        }

        public String toString() {
            String buff = "";

            // print line :
            buff += line.get(0) + " " + line.get(1);
            for (int i = 2; i < line.size(); i++)
                buff += ", " + line.get(i);
            buff += "\n";
            // you can uncomment the others set if you want to see them.
            // buff += "// REF      : " +  REF.toString() +"\n";
            // buff += "// DEF      : " +  DEF.toString() +"\n";
            // buff += "// PRED     : " +  PRED.toString() +"\n";
            // buff += "// SUCC     : " +  SUCC.toString() +"\n";
            buff += "// Life_IN  : " + Life_IN.toString() + "\n";
            buff += "// Life_OUT : " + Life_OUT.toString() + "\n";
            buff += "// Next_IN  : " + Next_IN.toString() + "\n";
            buff += "// Next_OUT : " + Next_OUT.toString() + "\n";
            return buff;
        }
    }

    private void compute_LifeVar() {
        // TODO: Implement LifeVariable algorithm on the CODE array (for machine code)
        // TODO: the returned variables should be added to the Life_OUT set of the last statement of the basic block (before the "ST" expressions in the machine code)

        Stack<MachLine> workList = new Stack<>();

        // Get last statement
        // TODO fix me when we add ST instructions
        MachLine lastLine = CODE.get(CODE.size() - 1);

        // Add the returned values to Life_out of the last statement
        lastLine.Life_OUT.addAll(RETURNED);

        workList.push(lastLine);


        while (!workList.empty()) {
            // node == worklist.pop
            MachLine line = (MachLine) workList.pop();

            if (!line.SUCC.isEmpty()) {
                Iterator iter = line.SUCC.iterator();
                if (iter.hasNext()) {
                    line.Life_OUT.addAll(CODE.get((Integer) iter.next()).Life_IN);
                }
            }

            // OLD IN = IN [ node ] ;
            HashSet oldIn = new HashSet();
            oldIn.addAll(line.Life_IN);

            // (OUT[ node ] − DEF[ node ] )
            HashSet newIn = new HashSet();
            newIn.addAll(line.Life_OUT);
            newIn.removeAll(line.DEF);

            // union REF[ node ]
            newIn.addAll(line.REF);
            line.Life_IN = newIn;

            if (!line.Life_IN.equals(oldIn)) {
                Iterator iterator = line.PRED.iterator();
                if (iterator.hasNext()) {
                    workList.push(CODE.get((Integer) iterator.next()));
                }
            }

        }
    }

    private void compute_NextUse() {
        Integer currentLineNumber = CODE.size() - 1;
        Stack workList = new Stack();
        workList.push(CODE.get(currentLineNumber));

        while (!workList.empty()) {

            // final line number for iteration
            final Integer lineNumber = currentLineNumber;

            // node = worklist.pop()
            MachLine line = (MachLine) workList.pop();

            // for(succ in successors(node) {union}
            if (!line.SUCC.isEmpty()) {
                Iterator iter = line.SUCC.iterator();
                // S'il y a un successeur, il ne devrait pas en avoir plus selon l'énoncé
                if (iter.hasNext()) {
                    // on récupere le next_in du successeur dans code et on ajoute toute les paires absentes de
                    // next_out[node] ( union out[node] et in[succnode]
                    CODE.get((Integer) iter.next()).Next_IN.nextuse.forEach((k, v) -> line.Next_OUT.nextuse.putIfAbsent(k, v));
                }
            }

            // NEXT_OLD_IN = NEXT_IN[node]
            //  *** WARNING BE CAREFULL WITH HASHMAP.CLONE()' IT IS SHALLOW COPYING ***
//            HashMap<String, ArrayList<Integer>> nextOldIn = (HashMap) line.Next_IN.nextuse.clone();


//            line.Next_IN.nextuse.forEach((k,v) -> {
//
//                nextOldIn.put(k,v);
//                if (lineNumber.equals(2)) {
//                    nextOldIn.get("a").add(666);
//                }
//
//            });

            // for v, n ou v est une variable de NEXT_OUT et n sont les numéros de ligne associés à cette variable
            line.Next_OUT.nextuse.forEach((v, n) -> { //for ((v, n) in NEXT_OUT[node])
                if (!line.DEF.contains(v)) { //if (v not in DEF[node])
                    line.Next_IN.nextuse.putIfAbsent(v, n); //NEXT_IN[node] = NEXT_IN[node] union {(v, n)}
                }
            });

            if (!line.REF.isEmpty()) {
                line.REF.forEach(var -> { // for (v in REF[node])
                    //NEXT_IN[node] = NEXT_IN [node] union {(v, current_line_number )}
                    if (line.Next_IN.nextuse.containsKey(var)) {
                        line.Next_IN.nextuse.get(var).add(lineNumber);
                    } else {
                        ArrayList<Integer> in = new ArrayList<>();
                        in.add(lineNumber);
                        line.Next_IN.nextuse.put(var, in);
                    }
                });
            }

            if (!line.Next_IN.nextuse.isEmpty()) { // if (NEXT_IN[node] != NEXT_OLD IN)
                line.PRED.forEach(pred -> workList.push(CODE.get(lineNumber -1))); // for (predNode in predecessors (node)) workList.push(predNode);
                currentLineNumber--;
            }

        }
    }

    public void compute_machineCode() {
        // TODO: Implement machine code with graph coloring for register assignation (REG is the register limitation)
        //       The pointers (ex: "@a") here should be replace by registers (ex: R0) respecting the coloring algorithm
        //       described in the TP requirements.
    }


    public List<String> set_ordered(Set<String> s) {
        // function given to order a set in alphabetic order TODO: use it! or redo-it yourself
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    // TODO: add any class you judge necessary, and explain them in the report. GOOD LUCK!
}
