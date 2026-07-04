% solveDAE Solve linear DAE E*x' + A*x = z(t) with singular E, invertible A
function [T,x] = solveDAE(E,A,z,t0,t1,h,x0)

    % Make time vector
    T = (t0:h:t1);
    N = length(T);
    % Initialize solution vector
    x = zeros(length(x0), N);
    x(:, 1) = x0;
    
    % compute pseudo inverse, to allow bringing the equation
    % E*x'+A'x=z into the form x'=E^+*(z-A*x)
    Einv = pinv(E);
    
    % solve for every t using backward euler
    % x_k+1 = x_k + h * f( x_k+1 )
    % x_k+1 = x_k + h * E^+ * ( z - A * x_k+1 )

    % substitute and precompute
    % x_k+1 = x_k + h * E^+ * z - h * E^+ * A * x_k+1
    % x_k+1 = v - h * E^+ * A * x_k+1
    % -> v = x_k + h * E^+ * z
    % h * E^+ * A * x_k+1 + x_k+1 = v
    % ( h * E^+ * A + I ) * x_k+1 = v
    % M * x_k+1 = v
    % -> M = h * E^+ * A + I
    M = h*Einv*A+eye(length(E));
    
    for k=1:N-1
        
        % solve using 
        % M * x_k+1 = v
        v = x(:,k)+h*Einv*z;
        x(:,k+1) = M\v;
        
    end
    
end
