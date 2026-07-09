% solveDAE Solve linear DAE E*x' + A*x = z(t) with singular E, invertible A
function [T,x] = solveDAEqz(E,A,z,t0,t1,h,x0)
    
    % Ensure z can be a constant vector
    if ~isa(z,'function_handle')
        z = @(t) z;
    end
    
    % Make time vector
    T = (t0:h:t1);
    N = length(T);
    
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
    tol = max(size(EE)) * eps(max(diagEE)); % TODO
    diffIdx = diagEE > tol;
    nd = sum(diffIdx);
    % Permutation to bring differential first
    perm = [find(diffIdx); find(~diffIdx)];
    AAp = AA(perm,perm);
    EEp = EE(perm,perm);
    Zp = Z(:,perm);
    Qp = Q(:,perm);
    
    % Partition transformed matrices
    Ad = AAp(1:nd,1:nd);
    Ed = EEp(1:nd,1:nd);
    Aa = AAp(nd+1:end,nd+1:end);
 %   Ea = EEp(nd+1:end,nd+1:end);
    % Extract off-diagonal blocks
    Ada = AAp(1:nd,nd+1:end);
    Aad = AAp(nd+1:end,1:nd);
    
    Aa_inv = inv(Aa);

    for k=1:N-1
        
        xp = x(:,k)
        zt = Qp'*z(T(k+1));
        yp = Zp\xp;
        ypd = yp(1:nd);
        zd = zt(1:nd);
        za = zt(nd+1:end);
        M = Ed/h+Ad-Ada*Aa_inv*Aad;
        rhs = zd+Ed*ypd/h+Ada*Aa_inv*za;
        yd = M\rhs;
        ya = Aa_inv*(za-Aad*yd);
        y = [yd; ya];
        x(:,k+1) = Zp * y;
        
    end

end