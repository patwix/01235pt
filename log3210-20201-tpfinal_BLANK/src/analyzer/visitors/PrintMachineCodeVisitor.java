package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {

    private PrintWriter m_writer = null;

    private Integer REG = 256; // default register limitation
    private ArrayList<String> RETURNED = new ArrayList<>(); // returned variables from the return statement
    private ArrayList<MachLine> CODE = new ArrayList<>(); // representation of the Machine Code in Machine lines (MachLine)
    private ArrayList<String> LOADED = new ArrayList<>(); // could be use to keep which variable/pointer are loaded/ defined while going through the intermediate code
    private ArrayList<String> MODIFIED = new ArrayList<>(); // could be use to keep which variable/pointer are modified while going through the intermediate code
    private HashSet<String> SPILLED = new HashSet<>();

    private HashMap<String, Integer> COLORMAP = new HashMap<>();

    private InterferenceGraph G;

    private HashMap<String, String> OP; // map to get the operation name from it's value

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OP = new HashMap<>();
        OP.put("+", "ADD");
        OP.put("-", "MIN");
        OP.put("*", "MUL");
        OP.put("/", "DIV");

    }

    private void ifVariableIsNotInMemoryLD(String var) {
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

    private void ifVariableValueModifiedST() {
        MODIFIED.forEach(v -> {
            ArrayList<String> line = new ArrayList<>();
            line.add("ST");
            line.add(v.substring(1, 2));
            line.add(v);
            MachLine ST = new MachLine(line);
            CODE.add(ST);
        });
    }

    private String generateRegisterName(Integer registerNumber) {
        return "R" + registerNumber.toString();
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

        // Process machine code do -> lifevar -> nextuse -> generategraph
        if (!MODIFIED.isEmpty()) {
            ifVariableValueModifiedST(); // Add ST instruction with my cool function
        }
        processMachineCode();
        compute_machineCode();

        for (int j = 0; j <  CODE.size(); j++) {
            for (int i = 0; i < CODE.get(j).line.size(); i++) {
                if(COLORMAP.containsKey(CODE.get(j).line.get(i))) {
                    CODE.get(j).line.set(i, generateRegisterName(COLORMAP.get(CODE.get(j).line.get(i))));
                }
            }
        }

        // generate the machine code from the CODE array (the CODE array should be transformed
        for (MachLine aCODE : CODE) m_writer.println(aCODE);
        return null;
    }

    private void processMachineCode() {
        compute_LifeVar(); // first Life variables computation (should be recalled when machine code generation)
        compute_NextUse(); // first Next-Use computation (should be recalled when machine code generation)
        generate_Graph(); // init G and add edges to G
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

    private void updateModifiedArray(String var) {
        if (RETURNED.contains(var) && !MODIFIED.contains(var)) {
            MODIFIED.add(var);
        }
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // On ne visite pas les enfants puisque l'on va manuellement chercher leurs valeurs
        // On n'a rien a transférer aux enfants
        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        updateModifiedArray(assigned); // add to modified if necessary
        ifVariableIsNotInMemoryLD(left); // check if variable is loaded and add it if not

        String right = (String) node.jjtGetChild(2).jjtAccept(this, null);
        ifVariableIsNotInMemoryLD(right); // check if variable is loaded and add it if not

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

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        updateModifiedArray(assigned); // add to modified if necessary

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        ifVariableIsNotInMemoryLD(left); // check if variable is loaded and add it if not

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

        String assigned = (String) node.jjtGetChild(0).jjtAccept(this, null);
        updateModifiedArray(assigned); // add to modified if necessary

        String left = (String) node.jjtGetChild(1).jjtAccept(this, null);
        ifVariableIsNotInMemoryLD(left); // check if variable is loaded and add it if not

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
        HashMap<String, ArrayList<Integer>> nextuse = new HashMap<String, ArrayList<Integer>>();

        NextUse() {
        }

        NextUse(HashMap<String, ArrayList<Integer>> nextuse) {
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
        HashSet<String> REF = new HashSet<>();
        HashSet<String> DEF = new HashSet<>();
        HashSet<Integer> SUCC = new HashSet<>();
        HashSet<Integer> PRED = new HashSet<>();
        HashSet<String> Life_IN = new HashSet<>();
        HashSet<String> Life_OUT = new HashSet<>();

        NextUse Next_IN = new NextUse();
        NextUse Next_OUT = new NextUse();

        MachLine(List<String> s) {
            this.line = s;
            int size = CODE.size();

            // PRED, SUCC, REF, DEF already computed (cadeau)

            // This is soooo fu***ng wrong, what if i'm inserting a ST line not at the end? I lost a couple hours trying to debug only
            // to find out that the problem was in provided code. WTF ! This is a poisoned gift .....
            if (size > 0) {
                PRED.add(size - 1);
                CODE.get(size - 1).SUCC.add(size);
            }

            // DAAMMMMIT i also lost a lot of time here to... ST and LD lines, ref / def ?
            if (OP.values().contains(s.get(0))) {
                this.DEF.add(s.get(1));
                for (int i = 2; i < s.size(); i++)
                    if (s.get(i).charAt(0) == '@')
                        this.REF.add(s.get(i));
            } else if (s.get(0).equals("LD")) {
                this.DEF.clear();
                this.DEF.add(s.get(1));
            } else {
                this.REF.clear();
                this.REF.add(s.get(2));
            }
        }

        // Better Machline constructor
        MachLine(List<String> s, Integer insertionIndex) {
            this.line = s;

            if (insertionIndex > 0) {
                CODE.get(insertionIndex - 1).SUCC.add(insertionIndex);
            }
            this.DEF.add(s.get(1));
            if (OP.values().contains(s.get(0))) {
                this.DEF.add(s.get(1));
                for (int i = 2; i < s.size(); i++)
                    if (s.get(i).charAt(0) == '@')
                        this.REF.add(s.get(i));
            } else if (s.get(0).equals("LD")) {
                this.DEF.clear();
                this.DEF.add(s.get(1));
            } else {
                this.REF.clear();
                this.REF.add(s.get(2));
            }
        }

        public String toString() {
            StringBuilder buff = new StringBuilder();

            // print line :
            buff.append(line.get(0)).append(" ").append(line.get(1));
            for (int i = 2; i < line.size(); i++)
                buff.append(", ").append(line.get(i));
            buff.append("\n");
            // you can uncomment the others set if you want to see them.
//            buff.append("// REF      : ").append(REF.toString()).append("\n");
//            buff.append("// DEF      : ").append(DEF.toString()).append("\n");
//            buff += "// PRED     : " + PRED.toString() + "\n";
//            buff += "// SUCC     : " + SUCC.toString() + "\n";
            buff.append("// Life_IN  : ").append(set_ordered(Life_IN).toString()).append("\n");
            buff.append("// Life_OUT : ").append(set_ordered(Life_OUT).toString()).append("\n");
            buff.append("// Next_IN  : ").append(Next_IN.toString()).append("\n");
            buff.append("// Next_OUT : ").append(Next_OUT.toString()).append("\n");
            return buff.toString();
        }

        void appendExclamationToNodeInLine(String node) {
            String op = line.get(0);
            for (int i = 1; i < line.size(); i++) {
                if (line.get(i).equals(node)) {
                    if (OP.values().contains(op)) {
                        line.set(i, node + "!");
                        if (i == 1) {
                            this.DEF.remove(node);
                            this.DEF.add(node + "!");
                        } else {
                            this.REF.remove(node);
                            this.REF.add(node + "!");
                        }
                    } else {
                        this.REF.clear();
                        this.DEF.clear();
                    }
                    if (op.equals("LD") && (i == 1)) {
                        line.set(i, node + "!");
                        this.DEF.add(node + "!");
                    } else if (op.equals("ST") && (i == 2)) {
                        line.set(i, node + "!");
                        this.REF.add(node + "!");
                    }
                }
            }
        }
    }

    // Vertex inner class
    private class Vertex {

        private String variableName;
        private HashMap<String, Boolean> adjencyArray;

        Vertex(ArrayList<String> variables, String name) {
            adjencyArray = new HashMap<>();
            variableName = name;

            // We init the adjency array with all known nodes set to false
            variables.forEach(watermelon -> {
                adjencyArray.put(watermelon, false);
            });
        }

        // Returns a set containing all registers used by neighbours
        HashSet<String> getRegistersUsedByNeighbours(InterferenceGraph g) {
            HashSet<String> usedRegisters = new HashSet<>();
            for(String nb: getAllNeighbours()) {
                if (COLORMAP.containsKey(nb)) {
                    usedRegisters.add(generateRegisterName(COLORMAP.get(nb)));
                }
            }
            return usedRegisters;
        }

        void addEdgeTo(String neighbourVariableName) {
            adjencyArray.replace(neighbourVariableName, true);
        }

        void removeEdgeTo(String neighbourVariableName) {
            adjencyArray.replace(neighbourVariableName, false);
        }

        Integer getAmountOfNeighbours() {
            return Collections.frequency(new ArrayList<>(adjencyArray.values()), true);
        }

        ArrayList<String> getAllNeighbours() {
            ArrayList<String> neighbours = new ArrayList<>();
            Iterator i = adjencyArray.keySet().iterator();
            while (i.hasNext()) {
                String nodeName = (String) i.next();
                if (adjencyArray.get(nodeName)) {
                    neighbours.add(nodeName);
                }
            }
            return neighbours;
        }

        public HashMap<String, Boolean> getAdjacencyArray() {
            return adjencyArray;
        }

        public String getNodeName() {
            return variableName;
        }
    }

    // Le Constructeur du graphe. Initialise la matrice d'ajacence avec des 0 partout
    private class InterferenceGraph {

        private HashMap<String, Vertex> adjacencyMatrix = new HashMap<>();
        InterferenceGraph(ArrayList<String> variables) {
            for (String v : variables) {
                adjacencyMatrix.put(v, new Vertex(variables, v));
            }
        }

        Vertex getVertex(String vertexName) {
            return adjacencyMatrix.get(vertexName);
        }

        // Ajouter une arête entre les noeuds de variables node1 et node2
        void addEdge(String node1, String node2) {
            this.getVertex(node1).addEdgeTo(node2);
            this.getVertex(node2).addEdgeTo(node1);
        }

        // Returns the name of the node with highest neighbours under REG value if it exists
        // If it do not exist, return "none" string
        String getNodeWithHighestNeigboursAmountUnderREG() {
            Integer bestNeighbourAmount = -1;
            String bestNode = "none";
            Iterator iter = set_ordered(this.adjacencyMatrix.keySet()).iterator();
            while (iter.hasNext()) {
                String var = (String) iter.next();
                Integer amountOfNeighbours = getVertex(var).getAmountOfNeighbours();
                if (amountOfNeighbours > bestNeighbourAmount && (amountOfNeighbours < REG)) {
                    bestNeighbourAmount = amountOfNeighbours;
                    bestNode = var;
                }
            }
            return bestNode;
        }

        String getNodeWithHighestNeigboursAmount() {
            Integer bestNeighbourAmount = -1;
            String bestNode = "none";

            Iterator iter = set_ordered(this.adjacencyMatrix.keySet()).iterator();
            while (iter.hasNext()) {
                String var = (String) iter.next();
                Integer amountOfNeighbours = getVertex(var).getAmountOfNeighbours();
                if (amountOfNeighbours > bestNeighbourAmount) {
                    bestNeighbourAmount = amountOfNeighbours;
                    bestNode = var;
                }
            }
            return bestNode;
        }

        HashMap<String, Vertex> getAdjacencyMatrix() {
            return adjacencyMatrix;
        }

        // Removes vertex from graph and all edges related to node in G
        // Returns the removed node
        // the returned node still has neighbours boolean set to true for later use
        Vertex removeNodeFromGraph(String node) {
            Vertex nodeToRemove = null;
            if (adjacencyMatrix.containsKey(node)) {
                nodeToRemove = adjacencyMatrix.remove(node);
                for (String s : this.adjacencyMatrix.keySet()) {
                    this.getVertex(s).removeEdgeTo(node);
                }
            }
            return nodeToRemove;
        }

        // We add back the node Vertex into G
        // for every neighbour node had before being spilled, we look in G and if it is present we add a edge between
        void addBackNodeAndConnectPreviousEdges(Vertex nodeToAddBack) {
            for (String otherNode : nodeToAddBack.getAdjacencyArray().keySet()) {

                // If we find true for a node in the adjencyArray and the other var exists in the graph
                // We add the edge between these nodes
                if (nodeToAddBack.getAdjacencyArray().get(otherNode) && this.adjacencyMatrix.containsKey(otherNode)) {
                    this.adjacencyMatrix.get(otherNode).addEdgeTo(nodeToAddBack.getNodeName());

                    // If old node isnt in G, we remove the old edge once and for all
                } else {
                    nodeToAddBack.removeEdgeTo(otherNode);
                }
            }
            // Now we add the node back into G
            this.adjacencyMatrix.put(nodeToAddBack.getNodeName(), nodeToAddBack);
        }
    }

    private void compute_LifeVar() {

        Stack<MachLine> workList = new Stack<>();

        // Get last statement
        MachLine lastLine = CODE.get(CODE.size() - 1);

        // Add the returned values to Life_out of the last statement
        lastLine.Life_OUT.addAll(RETURNED);
        lastLine.Life_OUT.removeAll(MODIFIED);

        // We need to save modified variables back in memory with ST instructions
        workList.push(lastLine);

        while (!workList.empty()) {
            // node == worklist.pop
            MachLine line = workList.pop();

            if (!line.SUCC.isEmpty()) {
                Iterator iter = line.SUCC.iterator();
                if (iter.hasNext()) {
                    line.Life_OUT.addAll(CODE.get((Integer) iter.next()).Life_IN);
                }
            }

            // OLD IN = IN [node] ;
            HashSet<String> oldIn = new HashSet<>(line.Life_IN);

            // (OUT[node] − DEF[node])
            HashSet<String> newIn = new HashSet<>(line.Life_OUT);
            newIn.removeAll(line.DEF);

            // union REF[node]
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

        for (int i = 0; i < CODE.size(); i++) {
            CODE.get(i).Next_IN.nextuse = new HashMap<>();
            CODE.get(i).Next_OUT.nextuse = new HashMap<>();
        }

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
                    CODE.get((Integer) iter.next()).Next_IN.nextuse.forEach((k, v) -> {
                        if (line.Next_OUT.nextuse.containsKey(k)) {

                            // use a set to perform union
                            Set<Integer> union = new HashSet<>();
                            union.addAll(v);
                            union.addAll(line.Next_OUT.nextuse.get(k));

                            // add the result array in NEXT_OUT[node]
                            ArrayList sorted = new ArrayList<>(union);
                            sorted.sort(null);
                            line.Next_OUT.nextuse.put(k, sorted);
                        } else {
                            v.sort(null);
                            line.Next_OUT.nextuse.put(k, v);
                        }
                    });
                }
            }

            HashMap<String, ArrayList<Integer>> nextOldIn = (HashMap) line.Next_IN.nextuse.clone();

            // for v, n ou v est une variable de NEXT_OUT et n sont les numéros de ligne associés à cette variable
            line.Next_OUT.nextuse.forEach((v, n) -> { //for ((v, n) in NEXT_OUT[node])
                if (!line.DEF.contains(v)) { //if (v not in DEF[node])

                    // use a set to perform union
                    Set<Integer> union = new HashSet<>();
                    if (!n.isEmpty()) {
                        union.addAll(n);
                        if (line.Next_IN.nextuse.containsKey(v)) {
                            union.addAll(line.Next_IN.nextuse.get(v));
                        }
                    }
                    ArrayList<Integer> sortedArray = new ArrayList<>(union);
                    sortedArray.sort(null);
                    // add the result array in NEXT_OUT[node]
                    line.Next_IN.nextuse.put(v, sortedArray); //NEXT_IN[node] = NEXT_IN[node] union {(v, n)}
                }
            });

            if (!line.REF.isEmpty()) {
                line.REF.forEach(var -> { // for (v in REF[node])
                    //NEXT_IN[node] = NEXT_IN [node] union {(v, current_line_number )}
                    if (line.Next_IN.nextuse.containsKey(var) && !line.Next_IN.nextuse.get(var).contains(lineNumber)) {
                        line.Next_IN.nextuse.get(var).add(lineNumber);
                        line.Next_IN.nextuse.get(var).sort(null);
                    } else {
                        ArrayList<Integer> in = new ArrayList<>();
                        in.add(lineNumber);
                        line.Next_IN.nextuse.put(var, in);
                    }
                });
            }

            if (!line.Next_IN.nextuse.equals(nextOldIn)) { // if (NEXT_IN[node] != NEXT_OLD IN)
                line.PRED.forEach(pred -> workList.push(CODE.get(lineNumber - 1))); // for (predNode in predecessors (node)) workList.push(predNode);
                currentLineNumber--;
            }
        }
    }

    private void generate_Graph() {

        // Find all nodes referenced in CODE MachLines.Next_OUTs
        HashSet<String> vertices = new HashSet<>();
        for (MachLine ml : CODE) {
            vertices.addAll(ml.Next_OUT.nextuse.keySet());
        }

        // Initialise G with a zero matrix
        G = new InterferenceGraph(new ArrayList<>(vertices));

        // Use CODE Next_OUTs to generate edges
        CODE.forEach(ml -> { // for each machline instructions in CODE
            ArrayList<String> nextOuts = new ArrayList<>(ml.Next_OUT.nextuse.keySet()); // create iterable array
            // Add all possible edges
            if (nextOuts.size() > 1) { // Obviously, we need 2 or more vertices to create edges, right?
                for (int i = 0; i < nextOuts.size() - 1; i++) {
                    for (int j = i + 1; j < nextOuts.size(); j++) {
                        G.addEdge(nextOuts.get(i), nextOuts.get(j));
                    }
                }
            }
        });
    }

    private void compute_machineCode() {

        // The stack we use for coloration
        Stack<Vertex> nodeStack = new Stack<>();

        // We copy G into a new graph object for processing

        // boucle while(!G.not empty)
        while (G.adjacencyMatrix.size() > 0) {

            // current node in iteration
            Vertex node;
            if (!G.getNodeWithHighestNeigboursAmountUnderREG().equals("none")) {
                String selectedNode = G.getNodeWithHighestNeigboursAmountUnderREG();
                node = G.removeNodeFromGraph(selectedNode);
            } else {
                // do spill
                String selectedNode = G.getNodeWithHighestNeigboursAmount();
                node = G.removeNodeFromGraph(selectedNode);
                if (!SPILLED.contains(selectedNode)) {

                    do_Spill(node);
                    processMachineCode();
                    compute_machineCode();
                    return;
                }
            }

            if (node != null) {
                nodeStack.push(node);
            }
        }

        // Boucle while (!stack.empty)
        while (!nodeStack.empty()) {
            Vertex popedNode = nodeStack.pop();

            G.addBackNodeAndConnectPreviousEdges(popedNode);
            Boolean registerFound = false;

            // Counter used to find first available register
            Integer color = 0;

            while (!registerFound) {
                if (!popedNode.getRegistersUsedByNeighbours(G).contains(generateRegisterName(color))) {

                    // If we find an available register, we assing it to popedNode and switch that bool to true to exit loop
                    registerFound = true;
                    // Otherwise we increment counter and try again
                } else {
                    color++;
                }
            }
            COLORMAP.put(popedNode.variableName, color);
        }
    }

    private void do_Spill(Vertex node) {

        // the first line where node is used
        Integer first = null;

        final String nodeName = node.getNodeName();
        SPILLED.add(nodeName);

        // numero de ligne de la premiere utilisation de node dans une expression ”OP” ( pas ”ST” ni ”LD” )
        for (int i = 0; i < CODE.size(); i++) {
            MachLine ml = CODE.get(i);
            // If a variable is at position 1 of an operation in OP before, or at, first, it is modified
            if (OP.values().contains(ml.line.get(0)) && ml.line.contains(nodeName)) {
                first = i;
                break;
            }
        }

        // Get node nextuses hashmap (all pointers)
        HashMap<String, ArrayList<Integer>> lineNextUses = CODE.get(first).Next_OUT.nextuse;

        Boolean isModified = false;
        for (int i = 0; i <= first; i++) {
            if (OP.containsValue(CODE.get(i).line.get(0)) && CODE.get(i).line.get(0).equals(nodeName)) {
                isModified = true;
            }
        }

        // Si node est modifiée, est dans returned et n'est pas réutilisée
        if (isModified && RETURNED.contains(nodeName) && lineNextUses.get(nodeName).isEmpty()) {
            // Node is modified -> Create new ST machline
            CODE.add(first + 1, new MachLine(new ArrayList<>(Arrays.asList(
                    "ST", nodeName.replaceAll("@", ""), nodeName)), first));
            fixBrokenPRED_SUCC();
        }

        // si node est réutilisée
        if (lineNextUses.containsKey(nodeName) && !lineNextUses.get(nodeName).isEmpty()) {

            if (isModified) {
                // Node is modified -> Create new ST machline
                CODE.add(first + 1, new MachLine(new ArrayList<>(Arrays.asList(
                        "ST", nodeName.replaceAll("@", ""), nodeName)), first));
                fixBrokenPRED_SUCC();
            }

            // prochaines utilisations du node
            ArrayList<Integer> nodeNextUsesAtLineFirst = lineNextUses.get(nodeName);

            // On recharge la node dans un nouveau node avant le prochain lieu de reutilisation
            Integer nextUse = lineNextUses.get(nodeName).get(0);
            CODE.add(nodeNextUsesAtLineFirst.get(0), new MachLine(new ArrayList<>(Arrays.asList(
                    "LD", nodeName + "!", nodeName.replaceAll("@", "").substring(0, 1))), nextUse));
            fixBrokenPRED_SUCC();

            // Pour toutes les reutilisations de node
            for (int i = nextUse; i < CODE.size(); i++) {
                CODE.get(i).appendExclamationToNodeInLine(nodeName);
            }

        } else {
            for (int i = first; i < CODE.size() - 1; i++) {

                // Si la ligne i est un ST, on l'enleve
                if (CODE.get(i).line.get(0).equals("ST") && CODE.get(i).line.get(2).equals(nodeName)) {
                    CODE.remove(i);
                }
            }
        }

        fixBrokenPRED_SUCC();
    }

    // if (cadeau) -> {fixBrokenPRED_SUCC()}
    private void fixBrokenPRED_SUCC() {
        for (int i = 0; i < CODE.size(); i++) {
            if (i > 0) {
                MachLine ml = CODE.get(i);
                ml.PRED.clear();
                ml.PRED.add(i - 1);
                CODE.get(i - 1).SUCC.clear();
                CODE.get(i - 1).SUCC.add(i);
            }
        }
    }

    private List<String> set_ordered(Set<String> s) {
        // function given to order a set in alphabetic order
        List<String> list = new ArrayList<>(s);
        Collections.sort(list);
        return list;
    }
}