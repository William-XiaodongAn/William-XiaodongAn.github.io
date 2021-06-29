function [U,T] = FHN(N)
U = [1];
V = [0];
T = [0];
epsilon = 0.01;
dt = 1;
a=0.1;
b=0.3;
du2dt = U(1)*(1.-U(1))*(U(1)-a) - V(1) ;
dv2dt = epsilon*(b*U(1)-V(1)) ;
for k = 1:N;
T(k+1) = k * dt;
U(k+1) = du2dt * dt + U(k);
V(k+1) = dv2dt * dt + V(k);
du2dt = U(k+1)*(1.-U(k+1))*(U(k+1)-a) - V(k+1) ;
dv2dt = epsilon*(b*U(k+1)-V(k+1)) ;
end
figure(1)
plot(T,U);
figure(2)
plot(T,V);