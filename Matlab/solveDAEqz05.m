% solveDAE Solve linear DAE E*x' + A*x = z(t) with singular E, invertible A
function [tv,x] = solveDAEqz(E,A,z,t0,t1,h,x0)

% Ensure z can be a constant vector
if ~isa(z,'function_handle')
    z = @(t) z;
end

% Make time vector
tv = (t0:h:t1);
N = length(tv);

% If x0 empty, compute consistent initial state by solving A*x0 = z(t0)
if isempty(x0)
    z0 = z(t0);
    x0 = A \ z0;
end

% Initialize solution vector
x = zeros(length(x0), N);
x(:, 1) = x0;

% Compute QZ factorization
[AA,EE,Q,Z] = qz(A,E,'real');

% Permute QZ form so that EE has leading block with nonzero diagonal (differential part)
% Identify indices where diagonal of EE is not (near) zero
diagEE = abs(diag(EE));
tol = max(size(EE)) * eps(max(diagEE));
diffIdx = diagEE > tol;
% Permutation to bring differential first
perm = [find(diffIdx); find(~diffIdx)];
AAp = AA(perm,perm);
EEp = EE(perm,perm);
Zp = Z(:,perm);
Qp = Q(:,perm);

for n=2:N

    % Get current simtime and transform last solution vector for QZ
    t = tv(n);
    % x_t = Z' * x(:,n-1); % optimization of Z \ x because Z is orthogonal and thus A^T = A^-1
    x_t = Zp \ x(:,n-1);

    % Solve DAE using QZ matrices
    M = (1/h)*EEp+AAp;
    v = (1/h)*EEp*x_t+Qp*z(t);
    xn_t = M \ v;

    x(:,n) = Zp * xn_t;

end

end