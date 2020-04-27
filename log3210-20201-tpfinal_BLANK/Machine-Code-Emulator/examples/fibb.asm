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
LD R4, b
// Life_IN  : [@i]
// Life_OUT : [@b, @i]
// Next_IN  : @a!:[10, 14]
// Next_OUT : @a!:[10, 14], @b:[2, 6, 7, 9, 10]

LD R2, a
// Life_IN  : [@b, @i]
// Life_OUT : [@a, @b, @i]
// Next_IN  : @a!:[10, 14], @b:[2, 6, 7, 9, 10]
// Next_OUT : @a:[2, 3, 10, 11], @a!:[10, 14], @b:[2, 6, 7, 9, 10]

ADD R0, R4, R2
// Life_IN  : [@a, @b, @i]
// Life_OUT : [@a, @b, @i, @t0]
// Next_IN  : @a:[2, 3, 10, 11], @a!:[10, 14], @b:[2, 6, 7, 9, 10]
// Next_OUT : @a:[3, 10, 11], @a!:[10, 14], @b:[6, 7, 9, 10], @t0:[4, 5]

LD R4, a
// Life_IN  : []
// Life_OUT : []
// Next_IN  :
// Next_OUT :

LD R2, d
// Life_IN  : [@a, @b, @i, @t0]
// Life_OUT : [@a, @b, @d, @i, @t0]
// Next_IN  : @a:[10, 11], @a!:[10, 14], @b:[6, 7, 9, 10], @t0:[4, 5]
// Next_OUT : @a:[10, 11], @a!:[10, 14], @b:[6, 7, 9, 10], @d:[4, 5, 9, 10], @t0:[4, 5]

MUL R1, R2, R0
// Life_IN  : [@a, @b, @d, @i, @t0]
// Life_OUT : [@a, @b, @d, @i, @t1]
// Next_IN  : @a!:[10, 11], @b:[6, 7, 9, 10], @d:[5, 9, 10], @t0:[4, 5]
// Next_OUT : @a!:[10, 11], @b:[6, 7, 9, 10], @d:[9, 10], @t1:[7, 8]

LD R3, c
// Life_IN  : [@a, @b, @d, @i, @t1]
// Life_OUT : [@a, @b, @c, @d, @i, @t1]
// Next_IN  : @a!:[10, 11], @b:[6, 7, 9, 10], @d:[9, 10], @t1:[7, 8]
// Next_OUT : @a!:[10, 11], @b:[6, 7, 9, 10], @c:[6, 7, 10, 11], @d:[9, 10], @t1:[7, 8]

MUL R0, R3, R4
// Life_IN  : [@a, @b, @c, @d, @i, @t1]
// Life_OUT : [@a, @b, @c, @d, @i, @t1, @t2]
// Next_IN  : @a!:[10, 11], @b:[7, 9, 10], @c:[7, 10, 11], @d:[9, 10], @t1:[7, 8]
// Next_OUT : @a!:[10, 11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t1:[7, 8], @t2:[7, 8]

ADD R0, R1, R0
// Life_IN  : [@a, @b, @c, @d, @i, @t1, @t2]
// Life_OUT : [@a, @b, @c, @d, @i, @t3]
// Next_IN  : @a!:[10, 11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t1:[7, 8], @t2:[7, 8]
// Next_OUT : @a!:[10, 11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t3:[8, 9]

ADD R0, #0, R0
// Life_IN  : [@a, @b, @c, @d, @i, @t3]
// Life_OUT : [@a, @b, @c, @d, @i, @t]
// Next_IN  : @a!:[10, 11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t3:[8, 9]
// Next_OUT : @a!:[10, 11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t:[13, 14]

MUL R1, R2, R4
// Life_IN  : [@a, @b, @c, @d, @i, @t]
// Life_OUT : [@a, @c, @d, @i, @t, @t4]
// Next_IN  : @a!:[11], @b:[9, 10], @c:[10, 11], @d:[9, 10], @t:[13, 14]
// Next_OUT : @a!:[11], @c:[10, 11], @t:[13, 14], @t4:[11, 12]

MUL R2, R3, R4
// Life_IN  : [@a, @c, @d, @i, @t, @t4]
// Life_OUT : [@c, @d, @i, @t, @t4, @t5]
// Next_IN  : @a!:[11], @c:[10, 11], @t:[13, 14], @t4:[11, 12]
// Next_OUT : @a!:[14], @t:[13, 14], @t4:[11, 12], @t5:[11, 12]

ADD R1, R1, R2
// Life_IN  : [@c, @d, @i, @t, @t4, @t5]
// Life_OUT : [@c, @d, @i, @t, @t6]
// Next_IN  : @a!:[14], @t:[13, 14], @t4:[11, 12], @t5:[11, 12]
// Next_OUT : @a!:[14], @t:[13, 14], @t6:[12, 13]

ADD R4, #0, R1
// Life_IN  : [@c, @d, @i, @t, @t6]
// Life_OUT : [@a, @c, @d, @i, @t]
// Next_IN  : @a!:[14], @t:[13, 14], @t6:[12, 13]
// Next_OUT : @a!:[15, 17], @t:[13, 14]

LD @a!!, a!
// Life_IN  : []
// Life_OUT : []
// Next_IN  :
// Next_OUT :

ADD R4, #0, R0
// Life_IN  : [@a, @c, @d, @i, @t]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @a!!:[15, 17], @t:[13, 14]
// Next_OUT : @a!!:[15, 17], @b:[15, 16, 18]

ST b, R4
// Life_IN  : [@b, @c, @d, @i]
// Life_OUT : [@c, @d, @i]
// Next_IN  : @b:[18]
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
// Life_IN  : [@a, @b]
// Life_OUT : [@a, @b, @c]
// Next_IN  :
// Next_OUT : @c:[1, 6]

MUL R0, #2, R2
// Life_IN  : [@a, @b, @c]
// Life_OUT : [@a, @b, @c, @t0]
// Next_IN  : @c:[1, 6]
// Next_OUT : @c:[6], @t0:[3]

LD R1, d
// Life_IN  : [@a, @b, @c, @t0]
// Life_OUT : [@a, @b, @c, @d, @t0]
// Next_IN  : @c:[6], @t0:[3]
// Next_OUT : @c:[6], @d:[3, 4, 7], @t0:[3]

ADD R0, R0, R1
// Life_IN  : [@a, @b, @c, @d, @t0]
// Life_OUT : [@a, @b, @c, @d, @t1]
// Next_IN  : @c:[6], @d:[3, 4, 7], @t0:[3]
// Next_OUT : @c:[6], @d:[4, 7], @t1:[4]

MUL R0, R1, R0
// Life_IN  : [@a, @b, @c, @d, @t1]
// Life_OUT : [@a, @b, @c, @d, @t2]
// Next_IN  : @c:[6], @d:[4, 7], @t1:[4]
// Next_OUT : @c:[6], @d:[7], @t2:[5]

ADD R3, #0, R0
// Life_IN  : [@a, @b, @c, @d, @t2]
// Life_OUT : [@a, @b, @c, @d, @t]
// Next_IN  : @c:[6], @d:[7], @t2:[5]
// Next_OUT : @c:[6], @d:[7], @t:[10]

MUL R2, R2, R2
// Life_IN  : [@a, @b, @c, @d, @t]
// Life_OUT : [@a, @b, @d, @t, @t3]
// Next_IN  : @c:[6], @d:[7], @t:[10]
// Next_OUT : @d:[7], @t:[10], @t3:[8]

MUL R0, R1, R1
// Life_IN  : [@a, @b, @d, @t, @t3]
// Life_OUT : [@a, @b, @t, @t3, @t4]
// Next_IN  : @d:[7], @t:[10], @t3:[8]
// Next_OUT : @t:[10], @t3:[8], @t4:[8]

ADD R0, R2, R0
// Life_IN  : [@a, @b, @t, @t3, @t4]
// Life_OUT : [@a, @b, @t, @t5]
// Next_IN  : @t:[10], @t3:[8], @t4:[8]
// Next_OUT : @t:[10], @t5:[9]

ADD R2, #0, R0
// Life_IN  : [@a, @b, @t, @t5]
// Life_OUT : [@a, @b, @c, @t]
// Next_IN  : @t:[10], @t5:[9]
// Next_OUT : @c:[14], @t:[10]

ADD R1, #0, R3
// Life_IN  : [@a, @b, @c, @t]
// Life_OUT : [@a, @b, @c, @d]
// Next_IN  : @c:[14], @t:[10]
// Next_OUT : @c:[14], @d:[15]

LD R0, i
// Life_IN  : [@a, @b, @c, @d]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @c:[14], @d:[15]
// Next_OUT : @c:[14], @d:[15], @i:[12]

DIV R0, R0, #2
// Life_IN  : [@a, @b, @c, @d, @i]
// Life_OUT : [@a, @b, @c, @d, @t6]
// Next_IN  : @c:[14], @d:[15], @i:[12]
// Next_OUT : @c:[14], @d:[15], @t6:[13]

ADD R0, #0, R0
// Life_IN  : [@a, @b, @c, @d, @t6]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @c:[14], @d:[15], @t6:[13]
// Next_OUT : @c:[14], @d:[15], @i:[16]

ST c, R2
// Life_IN  : [@a, @b, @c, @d, @i]
// Life_OUT : [@a, @b, @d, @i]
// Next_IN  : @c:[14], @d:[15], @i:[16]
// Next_OUT : @d:[15], @i:[16]

ST d, R1
// Life_IN  : [@a, @b, @d, @i]
// Life_OUT : [@a, @b, @i]
// Next_IN  : @d:[15], @i:[16]
// Next_OUT : @i:[16]

ST i, R0
// Life_IN  : [@a, @b, @i]
// Life_OUT : [@a, @b]
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