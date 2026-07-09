% solve_dae Solve linear DAE E*x' + A*x = z(t) with singular E, invertible A
% Inputs:
%   E   - n-by-n singular matrix
%   A   - n-by-n invertible matrix
%   zfun- function handle z(t) returning n-by-1 vector (or constant vector)
%   t0  - initial time
%   t1  - final time
%   h   - time step (positive)
%   x0  - initial state vector (n-by-1) consistent with algebraic constraints
% Outputs:
%   T   - column vector of times from t0 to t1 with step h
%   X   - matrix n-by-numel(T) with solution columns x(t)
function [T,X] = solveDAEqzAI(E,A,zfun,t0,t1,h,x0)
    % Ensure zfun can be a constant vector
    if ~isa(zfun,'function_handle')
        z_const = zfun;
        zfun = @(t) z_const;
    end
    % If x0 empty, compute consistent initial state by solving A*x0 = z(t0)
    if isempty(x0)
        z0 = zfun(t0);
        x0 = A \ z0;
    end
    
    % Time vector
    n = size(E,1);
    T = (t0:h:t1).';
    N = numel(T);
    X = zeros(n,N);
    X(:,1) = x0;
    
    % Precompute projector decomposition using QZ (generalized Schur) to handle singular E
    % We compute transformation to separate differential and algebraic parts.
    % Solve generalized eigenproblem pencil (A,E) via QZ: A*V - S*Q' with (S,T) convention
    [AA,EE,Q,Z] = qz(A,E,'real'); % AA,Q,Z such that Q'*A*Z = AA, Q'*E*Z = EE
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
    % Partition sizes
    nd = sum(diffIdx);
  %  na = n-nd;
    
    % Partition transformed matrices
    Ad = AAp(1:nd,1:nd);
    Ed = EEp(1:nd,1:nd);
    Aa = AAp(nd+1:end,nd+1:end);
    Ea = EEp(nd+1:end,nd+1:end); %#ok<NASGU>
    
    % Backtransform for reduced differential system:
    % Transform variables: x = Zp * y, Qp'*A*Zp = AAp_perm, Qp'*E*Zp = EEp_perm
    % Differential subsystem: Ed * y_d' + Ad * y_d + Ad_da * y_a = Qp(:,1:nd)'*z
    % Algebraic subsystem: Aa * y_a + Ad_ad * y_d = Qp(:,nd+1:end)'*z
    
    % Extract off-diagonal blocks
    Ad_da = AAp(1:nd,nd+1:end);
    Ad_ad = AAp(nd+1:end,1:nd);
    
    % Precompute inverses where needed
%    InvEd = inv(Ed);           % small nd-by-nd
    InvAa = inv(Aa);           % small na-by-na
    
    % Time integration: use implicit Euler on differential part and solve algebraic exactly each step
    for k=1:N-1
    %    t = T(k);
        tp = T(k+1);
   %     zt = zfun(t);
        ztp = zfun(tp);
        % transform z
      %  zt_t = Qp' * zt;
        ztp_t = Qp' * ztp;
        % current y
        y = Zp \ X(:,k); % since Zp is invertible, compute y = Zp^{-1} x
        y_d = y(1:nd);
    %    y_a = y(nd+1:end);
        % Implicit Euler for differential part: Ed*(y_d_new - y_d)/h + Ad*y_d_new + Ad_da*y_a_new = z_d_new
        % Algebraic part: Aa*y_a_new + Ad_ad*y_d_new = z_a_new
        z_d_new = ztp_t(1:nd);
        z_a_new = ztp_t(nd+1:end);
        % Solve algebraic for y_a_new in terms of y_d_new: y_a_new = InvAa*(z_a_new - Ad_ad*y_d_new)
        % Substitute into differential eq:
        % Ed/h*y_d_new - Ed/h*y_d + Ad*y_d_new + Ad_da*InvAa*(z_a_new - Ad_ad*y_d_new) = z_d_new
        % Collect terms for y_d_new:
        M = Ed/h + Ad - Ad_da * InvAa * Ad_ad;
        rhs = z_d_new + Ed*(y_d)/h + Ad_da * InvAa * z_a_new;
        y_d_new = M \ rhs;
        y_a_new = InvAa*(z_a_new - Ad_ad*y_d_new);
        y_new = [y_d_new; y_a_new];
        X(:,k+1) = Zp * y_new;
    end
    
end