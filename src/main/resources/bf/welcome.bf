WELCOME TO BRAINFUCK STUDIO
===========================

[[

BRAINFUCK OPERATORS

    >    increment the data pointer (to point to the next cell to the right)
    <    decrement the data pointer (to point to the previous cell to the left)
    +    increment the byte at the data pointer
    -    decrement the byte at the data pointer
    .    output the byte at the data pointer
    ,    accept one byte of input, storing its value in the byte at the data pointer
    [    if the byte at the data pointer is zero, then instead of moving the instruction pointer
         forward to the next command, jump it forward to the command after the matching ] command
    ]    if the byte at the data pointer is non-zero, then instead of moving the instruction pointer
         forward to the next command, jump it back to the command after the matching [ command


IDE SPECIFIC OPERATORS

    #    breakpoint - pauses the execution when encountered while debugging
                      this is a non-standard feature specific to Brainfuck Studio

]]


+++++++++[->+++++++++<]>++++++.<+++[->+++<]>+++++.+++++++.--
-------.<+++[->+++<]>+++.--.--------.<++++++++[->--------<]>
-----.<+++++++++[->+++++++++<]>+++.-----.<++++++++[->-------
-<]>---------------.<+++++[->+++++<]>+++++++++.<++++++[->+++
+++<]>++++++++++++.<++++[->----<]>-.++++++++.+++++.--------.
<+++[->+++<]>++++++.<++++[->----<]>--.++++++++.<++++++++[->-
-------<]>-----------.<+++++++[->+++++++<]>++.<+++++[->+++++
<]>++++++++.+.<++++[->----<]>-.+++++.++++++.<++++++++[->----
----<]>-.<+++++[->-----<]>--------.---.+++.---.<+++++++[->++
+++++<]>+++++++++++++.<++++++[->++++++<]>+++.+.<+++[->---<]>
--.<++++++++[->--------<]>-----.<+++++++++[->+++++++++<]>+++
+++++.<+++[->---<]>-.++++++.<+++++++++[->---------<]>----.<+
+++++++[->++++++++<]>+++++.+++++++++.----.+++++.<+++[->+++<]
>+.<+++++++++[->---------<]>--------.<+++++++++[->+++++++++<
]>++++.--.<+++[->---<]>-.+++++.-------.<++++++++[->--------<
]>-------.<++++++++[->++++++++<]>+++++++++.<+++[->+++<]>++.<
++++++++[->--------<]>------.<+++[->---<]>-----.<+++++[->+++
++<]>++++++++++.<++++++[->++++++<]>+.---..<+++[->+++<]>++++.
+.<+++++++++[->---------<]>-.<++++[->----<]>----.---.+++.---
.<++++[->++++<]>++++++................<+++[->+++<]>++++.<+++
[->---<]>----.<++++++[->++++++<]>++++++++++++.<+++++[->+++++
<]>+++++++++.<++++[->----<]>-.<++++[->++++<]>+++.<++++[->---
-<]>---.<+++[->+++<]>++++.+++++++.<+++++++++[->---------<]>-
---.<++++++[->++++++<]>+++++++++.<++++[->++++<]>++++.<+++[->
+++<]>++++.<+++[->---<]>-.---.<+++[->+++<]>++.<
