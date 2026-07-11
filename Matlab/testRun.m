clear all
close all

E1 = [0.000000    0.000000     0.000000
     0.000000    +0.047000    0.000000
     0.000000    0.000000     0.000000];

A1 = [+0.050000    -0.050000    +1.000000
      -0.050000    +0.050000    0.000000
      +1.000000    0.000000     0.000000];

E2 = [+0.047000    0.000000     0.000000
      0.000000     0.000000     0.000000
      0.000000     0.000000     0.000000];

A2 = [+0.050000    -0.050000    0.000000
      -0.050000    +0.050000    +1.000000
      0.000000     +1.000000    0.000000];

x0 = [0
      0
      0];

function [z]=ttt(t)
    if t < 8
        z = [0
     0
     1];
    else
        z = [0
     0
     5];
    end
end

[T1,X1] = solveDAEqz05(E1,A1, @ttt,0,20,1,x0)
[T2,X2] = solveDAEqz05(E2,A2, @ttt,0,20,1,x0)

tiledlayout(2,1)

% Top plot
nexttile
hold on
p1 = plot(T1, X1, "o-")
p2 = plot(T2, X2, "o-")
hold off
legend([p1, p2], [ ...
    "e1 Standard" "e2 Standard" "i Standard" ...
    "e1 Swapped"  "e2 Swapped" "i Swapped"...
    ]);
title('solveDAEqz05')

[T1,X1] = solveDAEqz04(E1,A1, @ttt,0,20,1,x0)
[T2,X2] = solveDAEqz04(E2,A2, @ttt,0,20,1,x0)

% Bottom plot
nexttile
hold on
p1 = plot(T1, X1, "o-")
p2 = plot(T2, X2, "o-")
hold off
legend([p1, p2], [ ...
    "e1 Standard" "e2 Standard" "i Standard" ...
    "e1 Swapped"  "e2 Swapped" "i Swapped"...
    ]);
title('solveDAEqz04')

