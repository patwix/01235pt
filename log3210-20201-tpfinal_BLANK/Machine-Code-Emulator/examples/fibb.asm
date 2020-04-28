// unsigned int fib(unsigned int n){
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
//    if (n <= 0)
//      return 0;
//    while (i > 0){
//      if (i % 2 == 1){
//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;
//      }
//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;
//    }
//    return a + b;
//  }

PRINT "Please enter the number of the fibonacci suite to compute:"
INPUT n

//    if (n <= 0)
//      return 0;
LD R0, n
BGTZ R0, validInput
PRINT #0
BR end

validInput:
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
DEC R0
ST i, R0
ST a, #1
ST b, #0
ST c, #0
ST d, #1

//    while (i > 0){
beginWhile:
LD R0, i
BLETZ R0, printResult

//      if (i % 2 == 1){
MOD R0, R0, #2
DEC R0
BNETZ R0, afterIf

CLEAR

//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;

// TODO:: PUT THE BLOCK 1 HERE !
LD R3, b
// Next_IN  : 
// Next_OUT : @b:[2, 6, 9]

LD R0, a
// Next_IN  : @b:[2, 6, 9]
// Next_OUT : @a:[2], @b:[2, 6, 9]

ADD R0, R3, R0
// Next_IN  : @a:[2], @b:[2, 6, 9]
// Next_OUT : @b:[6, 9], @t0:[4]

LD R1, d
// Next_IN  : @b:[6, 9], @t0:[4]
// Next_OUT : @b:[6, 9], @d:[4, 9], @t0:[4]

MUL R4, R1, R0
// Next_IN  : @b:[6, 9], @d:[4, 9], @t0:[4]
// Next_OUT : @b:[6, 9], @d:[9], @t1:[7]

LD R0, c
// Next_IN  : @b:[6, 9], @d:[9], @t1:[7]
// Next_OUT : @b:[6, 9], @c:[6, 11], @d:[9], @t1:[7]

MUL R2, R0, R3
// Next_IN  : @b:[6, 9], @c:[6, 11], @d:[9], @t1:[7]
// Next_OUT : @b:[9], @c:[11], @d:[9], @t1:[7], @t2:[7]

ADD R2, R4, R2
// Next_IN  : @b:[9], @c:[11], @d:[9], @t1:[7], @t2:[7]
// Next_OUT : @b:[9], @c:[11], @d:[9], @t3:[8]

ADD R2, #0, R2
// Next_IN  : @b:[9], @c:[11], @d:[9], @t3:[8]
// Next_OUT : @b:[9], @c:[11], @d:[9], @t:[14]

MUL R1, R1, R3
// Next_IN  : @b:[9], @c:[11], @d:[9], @t:[14]
// Next_OUT : @c:[11], @t:[14], @t4:[12]

LD R4, a
// Next_IN  : @c:[11], @t:[14], @t4:[12]
// Next_OUT : @a!:[11], @c:[11], @t:[14], @t4:[12]

MUL R0, R0, R4
// Next_IN  : @a!:[11], @c:[11], @t:[14], @t4:[12]
// Next_OUT : @t:[14], @t4:[12], @t5:[12]

ADD R0, R1, R0
// Next_IN  : @t:[14], @t4:[12], @t5:[12]
// Next_OUT : @t:[14], @t6:[13]

ADD R4, #0, R0
// Next_IN  : @t:[14], @t6:[13]
// Next_OUT : @a!:[15], @t:[14]

ADD R3, #0, R2
// Next_IN  : @a!:[15], @t:[14]
// Next_OUT : @a!:[15], @b:[16]

ST a, R4
// Next_IN  : @a!:[15], @b:[16]
// Next_OUT : @b:[16]

ST b, R3
// Next_IN  : @b:[16]
// Next_OUT : 

// TODO:: END THE BLOCK 1 HERE ABOVE !

CLEAR

afterIf:
CLEAR

//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;

// TODO:: PUT THE BLOCK 2 HERE !
LD R2, c
// Next_IN  : 
// Next_OUT : @c:[1, 6]

MUL R0, #2, R2
// Next_IN  : @c:[1, 6]
// Next_OUT : @c:[6], @t0:[3]

LD R1, d
// Next_IN  : @c:[6], @t0:[3]
// Next_OUT : @c:[6], @d:[3, 4, 7], @t0:[3]

ADD R0, R0, R1
// Next_IN  : @c:[6], @d:[3, 4, 7], @t0:[3]
// Next_OUT : @c:[6], @d:[4, 7], @t1:[4]

MUL R0, R1, R0
// Next_IN  : @c:[6], @d:[4, 7], @t1:[4]
// Next_OUT : @c:[6], @d:[7], @t2:[5]

ADD R3, #0, R0
// Next_IN  : @c:[6], @d:[7], @t2:[5]
// Next_OUT : @c:[6], @d:[7], @t:[10]

MUL R2, R2, R2
// Next_IN  : @c:[6], @d:[7], @t:[10]
// Next_OUT : @d:[7], @t:[10], @t3:[8]

MUL R0, R1, R1
// Next_IN  : @d:[7], @t:[10], @t3:[8]
// Next_OUT : @t:[10], @t3:[8], @t4:[8]

ADD R0, R2, R0
// Next_IN  : @t:[10], @t3:[8], @t4:[8]
// Next_OUT : @t:[10], @t5:[9]

ADD R2, #0, R0
// Next_IN  : @t:[10], @t5:[9]
// Next_OUT : @c:[14], @t:[10]

ADD R1, #0, R3
// Next_IN  : @c:[14], @t:[10]
// Next_OUT : @c:[14], @d:[15]

LD R0, i
// Next_IN  : @c:[14], @d:[15]
// Next_OUT : @c:[14], @d:[15], @i:[12]

DIV R0, R0, #2
// Next_IN  : @c:[14], @d:[15], @i:[12]
// Next_OUT : @c:[14], @d:[15], @t6:[13]

ADD R0, #0, R0
// Next_IN  : @c:[14], @d:[15], @t6:[13]
// Next_OUT : @c:[14], @d:[15], @i:[16]

ST c, R2
// Next_IN  : @c:[14], @d:[15], @i:[16]
// Next_OUT : @d:[15], @i:[16]

ST d, R1
// Next_IN  : @d:[15], @i:[16]
// Next_OUT : @i:[16]

ST i, R0
// Next_IN  : @i:[16]
// Next_OUT : 

// TODO:: END THE BLOCK 2 HERE ABOVE!




// TODO:: This instruction is just a placeholder to let the code end, remove the code below!
//LD R0, i
//DEC R0
//ST i, R0
// TODO:: Remove the placeholder above of this line!

CLEAR
BR beginWhile

//    return a + b;
printResult:
LD R0, a
LD R1, b
ADD R0, R0, R1
PRINT R0

end:
PRINT "END"