import java.io.*;
import java.awt.*;
import java.applet.*;
import java.net.*;
import java.awt.event.*;
import java.lang.*;
import java.awt.image.MemoryImageSource;
import java.awt.image.IndexColorModel;


/*      <applet code="vent2" width=400 height=400>
 </applet>
 */


public class vent2 extends Applet  
implements MouseListener, Runnable{
	
	int nspeed = 2;
	int nV = 887; //number of vertices
	int nEle = 1621; //number of elements
	int ndx = 871; //number of unknown dx
	int ndy = 879; //number of unknown dy
	//------ Location of Vertices in x&y (mm) ----------------
	double cx = 0.1; //scaling of vertices in x (mm)
	double cy = 0.1; //scaling of vertices in y (mm)
	int Nx[] = new int[nV];
	int Ny[] = new int[nV];
	//------ Vertices of each Element ---------
	int v1[] = new int[nEle];
	int v2[] = new int[nEle];
	int v3[] = new int[nEle];
	//------ Unknown Vertices not defined by Dirchlet Boundary Conditions ---------
	int vdx[] = new int[ndx]; 
	int vdy[] = new int[ndy]; 
	//------ Laplacian-like Matrix ---------------
	double[][] Kee = new double[nV][nV];
	//------ Stiffness Matrix (solving only for uknown dx & dy) -------------------
	double[][] kmm = new double[ndx+ndy][nEle+nV*2];
	//---------- Active Stress --------------
	double T22[] = new double[nEle];
	//---------- Neumann Boundary Conditions -----------
	double tb1[] = new double[nV];
	double tb2[] = new double[nV];
	//---------- Displacements ------------
	double dx[] = new double[nV];
	double dy[] = new double[nV];
	
	double val=0;
	int i=0;
	int j=0;
	int xpoints[] = new int[3];
	int ypoints[] = new int[3]; 
	int sxy = 1; //how much to scale the nodes for display
	int tlx = 60; //translation of nodes in x for display
	int tly = 30; //translaiton of nodes in y for display
	int npoints = 3;
	
	int Nclrs = 236;          // the number of colors created
	int iclr;
	Color[] fcmap;
	
	Image buffer;                    
	Graphics2D big;
	Graphics bufferGraphics;        
	Dimension size;  // The size of the applet
	
	//----------matrices for the calculations--------
	//double  k = 8., a = 0.15, eps0 = 0.002, mu1 = 0.2, mu2 = 0.3, eps;
	double eps=0.002;
	//double eps=.01; //default value
	double gamma=1.,beta=.5, a=0.1,delta=0;
	double u[]=new double[nV],v[]=new double[nV];
	double ut[]=new double[nV];
	double jcrt[] = new double[nV];

	Graphics g1;
	Point p;
	double ppx,ppy;
	
	Thread  thread;
	boolean down;
	
	double dt=0.1;
	
	
	
	//--------initialize and reads the nodes and vertices------------
	public void init()
	{
		readData();
		Thread thread = new Thread(this);
		thread.start();
		size = this.size();
		setBackground( Color.white );
		buffer = this.createImage(size.width, size.height);
		
		loadfcmap();
		
		bufferGraphics = buffer.getGraphics();
		
		addMouseListener(this);
		
		
		//----------Initialize Electrical ICs-------------
		for(i=0;i<nV;i++){
			v[i] = 0;
			u[i] = 0;
			ut[i] = 0;
		}  
		//----------Initialize Mechanical ICs-------------
		for(i=0;i<nV;i++){
			tb1[i] = 0;
			tb2[i] = 0;
			dx[i] = 0;
			dy[i] = 0;
		} 
		for(i=0;i<nEle;i++){
			T22[i] = 0;
		}
		
		//------ Initial activation at apex--------
		for(i=0;i<nV;i++){
			//if(Nx[i] > 5 && Nx[i]<10 && Ny[i]>4 && Ny[i]<8) {
			if(Ny[i]>300){
				u[i] = 1;
			}  
		}
	}
	
	public void mouseClicked(MouseEvent me){
	}
	public void mouseEntered(MouseEvent me){
	}
	public void mouseExited(MouseEvent me){
	}
	public void mousePressed(MouseEvent me){
		p=me.getPoint();
		ppy=(int)((p.y/sxy-tly));
		ppx=(int)((p.x/sxy-tlx));
		int cpt = 25;
		for(i=0; i<nV; i++){ 
			if((ppx>Nx[i]-cpt && ppx<Nx[i]+cpt) && (ppy>Ny[i]-cpt && ppy<Ny[i]+cpt)){
				u[i]=1;
			}
		} 
		System.out.println(ppy);
		System.out.println(ppx);
	}
	public void mouseReleased(MouseEvent me){
		//   down=false;
	}
	
	//-----------draws the mesh on screen----------------
	public void paint(Graphics g){
		//Graphics2D g2 = (Graphics2D) g;
		bufferGraphics.setColor(Color.white);
		bufferGraphics.fillRect(0,0,size.width, size.height);
		
		for (int k=0; k<15; k++){    
			//-------- Solve for Current ---------
			for (j=0; j<nV; j++){ 				
				//eps = eps0 + mu1*v[j]/( u[j] + mu2);
				//v[j] = v[j] + dt*eps*(-v[j] - k*u[j]*(u[j] - a - 1));
				//jcrt = k*u[j] * (u[j] - a) * (u[j] - 1.0) - u[j]*v[j];
				v[j] = v[j] + dt*eps*(beta*u[j] - gamma*v[j] - delta);
				jcrt[j] = -(u[j] * (a - u[j]) * (u[j] - 1.0) - v[j]);
				
			}
			//--------- Compute Membrane Potential using Diffusion Equation ----------
			for (i=0; i<nV; i++){
				for (j=0; j<nV; j++){
					ut[i] = ut[i] + Kee[i][j]*(u[j] - dt*jcrt[j]);
				}
				//System.out.println(ut[i]);
			}
			for (i=0; i<nV; i++){
				u[i]=ut[i];
				ut[i] = 0;
			}
		}
		//------------  Compute active stress(only along e2-axis or y-axis) -----------------
		for (i=0; i<nEle; i++){
			double magTa = 3;
			T22[i] =  magTa*(u[v1[i]] + u[v2[i]] + u[v3[i]])/3;
		}
		//------------ Compute dx using stiffness equation ------------------
		for (i=0; i<ndx; i++){
			for (j=0; j<nEle; j++) //Add contributions from T22
				val = val + kmm[i][j] * T22[j];
			for (j=0; j<nV; j++) //Add contributions from tb1
				val = val + kmm[i][j + nEle] * tb1[j];
			for (j=0; j<nV; j++) //Add contributions from tb2
				val = val + kmm[i][j + nEle + nV] * tb2[j];
			dx[vdx[i]] = val;
			val = 0;
		}
		//------------ Compute dy using stiffness equation ------------------
		for (i=0; i<ndy; i++){
			for (j=0; j<nEle; j++) //Add contributions from T22
				val = val + kmm[i + ndx][j] * T22[j];
			for (j=0; j<nV; j++) //Add contributions from tb1
				val = val + kmm[i + ndx][j + nEle] * tb1[j];
			for (j=0; j<nV; j++) //Add contributions from tb2
				val = val + kmm[i + ndx][j + nEle + nV] * tb2[j];
			dy[vdy[i]] = val;
			val = 0;
		}
		
		
		for (i=0; i<nEle; i++){
			
			xpoints[0] = tlx + sxy*Nx[v1[i]] + (int)(sxy*dx[v1[i]]/cx);
		    ypoints[0] = tly + sxy*Ny[v1[i]] + (int)(sxy*dy[v1[i]]/cy);
			xpoints[1] = tlx + sxy*Nx[v2[i]] + (int)(sxy*dx[v2[i]]/cx);
			ypoints[1] = tly + sxy*Ny[v2[i]] + (int)(sxy*dy[v2[i]]/cy);
			xpoints[2] = tlx + sxy*Nx[v3[i]] + (int)(sxy*dx[v3[i]]/cx);
			ypoints[2] = tly + sxy*Ny[v3[i]] + (int)(sxy*dy[v3[i]]/cy);
			
			//System.out.println(u[v1[i]]);
			
		    iclr = (int)((u[v1[i]]+0.3)/1.3*(Nclrs)-1);
			//System.out.println(u[v1[i]]);
			bufferGraphics.setColor(fcmap[iclr]);
			bufferGraphics.fillPolygon( xpoints, ypoints, npoints);
			//}
			//else{
			//	bufferGraphics.setColor(Color.white);
			//	bufferGraphics.fillPolygon( xpoints, ypoints, npoints);
			//}
			
			
			bufferGraphics.setColor(Color.black);
			bufferGraphics.drawLine(xpoints[0],ypoints[0],xpoints[1],ypoints[1]);
			bufferGraphics.drawLine(xpoints[0],ypoints[0],xpoints[2],ypoints[2]);
			bufferGraphics.drawLine(xpoints[1],ypoints[1],xpoints[2],ypoints[2]);
			
			
		}
		
		g.drawImage(buffer, 0, 0, this);		
		
	}
	
	private void loadfcmap()
	{
		fcmap = new Color[242];
	    //Flavio's Color Map
		float red = 0;
		float green = (float)3/256;
		float blue = (float)6.6/256;
		int n = 0;;
		//for (int i=0; i<6; i++){
		//	fcmap[n] = new Color(red, green, blue);
		//	n++;
		//}
		
		red = 0;
		green = (float)50/256;
		blue = (float)100/256;
		for (int i=0; i<38; i++){
			fcmap[n] = new Color(red, green, blue);
			green = green + (float)2.7/256;
			n++;
		}
		
		red = 0;
		green = (float)140/256;
		blue = (float)100/256;
		for (int i=0; i<37; i++){
			fcmap[n] = new Color(red,green,blue);
			blue = blue - (float)2.7/256;
			n++;
		}
		
		red = 0;
		green = (float)140/256;
		blue = 0;
		for (int i=0; i<19; i++){
			fcmap[n] = new Color(red,green,blue); 
			red = red + (float)6.58/256;
			green = green - (float)2.23/256;
			n++;
		}
		
		red = 0;
		green = (float)120/256;
		blue = (float)100/256;
		for (int i=0; i<19; i++){
			fcmap[n] = new Color(red,green,blue); 
			red = red + (float)4.41/256;
			green = green - (float)5.52/256;
			n++;
		}
		
		red = (float)200/256;
		green = 0;
		blue = 0;
		for (int i=0; i<19; i++){
			fcmap[n] = new Color(red,green,blue); 
			green = green + (float)2/256;
			n++;
		}
		
		red = (float)209/256;
		green = (float)36/256;
		blue = 0;
		for (int i=0; i<37; i++){
			fcmap[n] = new Color(red,green,blue); 
			red = red + (float)0.441/256;
			green = green + (float)2/256;
			n++;
		}
		
		red = (float)224/256;
		green = (float)110/256;
		blue = 0;
		for (int i=0; i<67; i++){
			fcmap[n] = new Color(red,green,blue); 
			red = red + (float)0.4166/256;
			green = green + (float)1.883/256;
			n++;
		}
	}
	
	//--------------reads data----------------------
	private void readData()
	{
		try
		{    
			//---------reads nodes --------------------------
			URL url1 = new URL(getCodeBase(),"readin/grid/x(887)");
			DataInputStream dataIn1 = new DataInputStream(url1.openStream());
            for (int i=0; i<nV; i++){
				
				Nx[i] = Integer.parseInt(dataIn1.readLine());
				//           System.out.println( Nx[i] ); 
			}
			dataIn1.close();
			
			URL url2 = new URL(getCodeBase(),"readin/grid/y(887)");
			DataInputStream dataIn2 = new DataInputStream(url2.openStream());
            for (int i=0; i<nV; i++){
				
				Ny[i] = Integer.parseInt(dataIn2.readLine());
				//           System.out.println( Nx[i] + "," + Ny[i] );
			}
			dataIn2.close();
			////--------------------------Read Vertices-----------------------
			URL urlv1 = new URL(getCodeBase(),"readin/grid/v1(1621)");
			DataInputStream datav1 = new DataInputStream(urlv1.openStream());
            for (int i=0; i<nEle; i++)
				v1[i] = Integer.parseInt(datav1.readLine()) - 1;

			
			URL urlv2 = new URL(getCodeBase(),"readin/grid/v2(1621)");
			DataInputStream datav2 = new DataInputStream(urlv2.openStream());
            for (int i=0; i<nEle; i++)
				v2[i] = Integer.parseInt(datav2.readLine()) - 1;
			datav2.close();
			
			URL urlv3 = new URL(getCodeBase(),"readin/grid/v3(1621)");
			DataInputStream datav3 = new DataInputStream(urlv3.openStream());
            for (int i=0; i<nEle; i++)
				v3[i] = Integer.parseInt(datav3.readLine()) - 1;
			datav3.close();
			//-------------------- Read Dirchlet Boundary Conditions ---------------
			URL urlv4 = new URL(getCodeBase(),"readin/mtrx/2DVentricle_vdy(879).txt");
			DataInputStream datav4 = new DataInputStream(urlv4.openStream());
            for (int i=0; i<ndy; i++){
				vdy[i] = Integer.parseInt(datav4.readLine()) - 1;
				//System.out.println( vdy[i] ); 
			}
			datav4.close();
			
			URL urlv5 = new URL(getCodeBase(),"readin/mtrx/2DVentricle_vdx(871).txt");
			DataInputStream datav5 = new DataInputStream(urlv5.openStream());
            for (int i=0; i<ndx; i++)
				vdx[i] = Integer.parseInt(datav5.readLine()) - 1;
			datav5.close();
			
			//----------------- Read Laplacian-Like matrix ----------------
			int r=0, c=0;
			BufferedReader in1 = new BufferedReader(new FileReader("readin/mtrx/2DVentricle_Kee(887x887)_c(0.1,0.1)_dt(0.1)_D(0.1171).txt"));	//reading files in specified directory
			String line1;
			while ((line1 = in1.readLine()) != null){
				String[] values1 = line1.split(",");
				c = 0;
				for (String str1 : values1){
					double str_double1 = Double.parseDouble(str1);
					Kee[r][c]=str_double1;
					//System.out.println(Kee[r][c]);
					c=c+1;
					//System.out.println(c);
				}
				r=r+1;	
			}	
			in1.close();
			//----------------- Read Stiffness matrix ----------------
			r=0; c=0;
			BufferedReader in2 = new BufferedReader(new FileReader("readin/mtrx/2DVentricle_kmm(1750x3395)_c(0.1,0.1).txt"));	//reading files in specified directory
			String line2;
			while ((line2 = in2.readLine()) != null){
				String[] values2 = line2.split(",");
				c = 0;
				for (String str2 : values2){
					double str_double2 = Double.parseDouble(str2);
					kmm[r][c]=str_double2;
					//System.out.println(Kee[r][c]);
					c=c+1;
					//System.out.println(c);
				}
				r=r+1;	
			}	
			in2.close();
		}
		catch(IOException e){
			System.out.println("Problem findisssng file");
		}
	}
	
	public void run() {
		try {
			while(true){
				Thread.sleep(nspeed);
				//	    if(!down) {
				repaint();
				//		}
			}
		}
		catch(Exception e){
		}
	}
	
}

