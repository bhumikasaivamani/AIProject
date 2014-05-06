

import java.io.*;
import java.util.*;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.ListModel;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;


public class  OwlReader 
{
	
	MainFrame mf;
	boolean buttonPressed;
	GUIThread gui;
	public OwlReader()
	{
		buttonPressed = false;
		
		gui = new GUIThread(this);
		new Thread(gui).start();
	}

	/**
	 * Function to read the OWL File
	 * @return - a Model is returned
	 */
	public Model readOwlFile()
	{
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open( "Ontology.owl" );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: not found");
		}

		// read the RDF/XML file
		model.read(in, null);
		return model;
	}
	
/**
 * Function that adds new disease to the owl File.System keeps on learning new disease
 * @param drugs
 * @param symptoms
 */
	public void extendModel_NewDisease(ArrayList<String> drugs,List<String> symptoms)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		Model model = null;
		InputStream in = null;
		try 
		{
			in = new FileInputStream(new File("Ontology.owl"));
			model = ModelFactory.createOntologyModel();
			model.read(in,defaultNameSpace); 
			in.close();
	   } 
	   catch (Exception e) 
	   {
          e.printStackTrace();
       }
		//String drug=newSideEffects.get(newSideEffects.size()-1);
		String disease="";
		for(int k=0;k<symptoms.size();k++)
		{
		disease=disease+symptoms.get(k);	
		}
		for(int i=0;i<drugs.size();i++)
		{
			String temp=defaultNameSpace+drugs.get(i);
			Resource r1 =model.createResource(defaultNameSpace+disease)
					.addProperty(model.createProperty(defaultNameSpace+"hasDrug"),model.createResource(temp));
			writeModel("Ontology.owl", "RDF/XML-ABBREV", model);
		}
		
		for(int i=0;i<symptoms.size();i++)
		{
			String temp=defaultNameSpace+symptoms.get(i);
			Resource r2 =model.createResource(defaultNameSpace+disease)
					.addProperty(model.createProperty(defaultNameSpace+"hasSymptom"),model.createResource(temp));
			writeModel("Ontology.owl", "RDF/XML-ABBREV", model);
		}
	}

	/**
	 * Function that adds new side effects to the owl file
	 * @param newSideEffects
	 */
	public void extendModel(ArrayList<String> newSideEffects)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		Model model = null;
		InputStream in = null;

		try 
		{
			in = new FileInputStream(new File("Ontology.owl"));
		    model = ModelFactory.createOntologyModel();
			model.read(in,defaultNameSpace); 
			in.close();
	   } 
	   catch (Exception e) 
	   {
          e.printStackTrace();
       }
		String drug=newSideEffects.get(newSideEffects.size()-1);
		for(int i=0;i<newSideEffects.size()-1;i++)
		{
			String temp=defaultNameSpace+newSideEffects.get(i);
			Resource r1 =model.createResource(defaultNameSpace+drug)
					.addProperty(model.createProperty(defaultNameSpace+"hasSideEffect"),model.createResource(temp));
			writeModel("Ontology.owl", "RDF/XML-ABBREV", model);
		}
	}
		
	/**
	 * Function that writes the model in to File
	 * @param fileName
	 * @param format
	 * @param m
	 */
	public void writeModel(String fileName, String format, Model m)
	{
		try
		{
			PrintWriter writer = new PrintWriter(fileName, "UTF-8");
			
			m.write(writer,format);
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	
	/**
	 * Function that gets symptoms from the user and gives drug list and disease as output.Calls several other functions.
	 * @param symptoms
	 */
	void queryModel(List<String> symptoms)
	{

		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		String hasSymptomNameSpace="<http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#hasSymptom>";
		
		ArrayList<ArrayList<String>> DList = new ArrayList<ArrayList<String>>();
		ArrayList<String> AllergicDrugs=new ArrayList<String>();
		Model model = null;
		InputStream in = null;
		String temp ="";
		try 
		{
			in = new FileInputStream(new File("Ontology.owl"));
			model = ModelFactory.createOntologyModel();
			model.read(in,defaultNameSpace); 
			in.close();
		} 
		catch (Exception e) 
		{

          e.printStackTrace();
		}

		/****************************************Getting Diseases*********************************************************/
		
		for(int i=0;i<symptoms.size();i++)
		{
			String ntemp="<"+defaultNameSpace+symptoms.get(i)+">";
			temp =temp+"   ?Disease "+hasSymptomNameSpace+" " +ntemp + " ." ;
		}
		String queryStringDisease ="SELECT ?Disease "+
						"WHERE {"+ temp +
						"   }";
				
		//System.out.println(queryStringDisease);
		List<String> Diseases=issueSPARQL_Diseases(queryStringDisease, model);
		System.out.println(Diseases);
		String disease = filterDisease(model, new ArrayList<>(Diseases));
		if(disease!=null)
		{
		
		
		/****************************************Getting Drugs*********************************************************/
		
		String dtemp="<"+defaultNameSpace+disease+">";
		String queryStringDrug =
		"SELECT ?Drug " +
				"WHERE {" +
				"     " +dtemp+ " <http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#hasDrug>  ?Drug ." +
				"      }";
		
		//System.out.println(queryStringDrug);
		ArrayList<String> Drugs=issueSPARQLDrug(queryStringDrug, model);
		
		//Drugs and side effects in list of lists
		for(int j=0;j<Drugs.size();j++)
        {
			DList.add(getSideeffects(model,Drugs.get(j)));
        }
		
		System.out.println("DrugList :");
		for(int k=0;k<DList.size();k++)
		{
				System.out.println(DList.get(k).get(0));
		}
				
		//System.out.println("Did u have side effects with any of the drugs ?");
		gui.m.jLabel2.setText("Did u have side effects with any of the drugs ?");
		gui.m.jTextArea1.setText("");
		gui.m.jPanel5.setVisible(true);
		DefaultListModel test = new DefaultListModel();

		for(int k=0;k<DList.size();k++)
		{
				test.addElement(DList.get(k).get(0));
		}
		gui.m.jList1.setModel(test);
		gui.m.jList1.setEnabled(false);
		//Scanner sc=new Scanner(System.in);
		while(!buttonPressed)
		{
			System.out.print("");
		}
		//System.out.println("Button Pressed");
		buttonPressed=false;
		
		if(gui.m.jTextArea1.getText().equalsIgnoreCase("yes"))
		{	
			String reply = "yes";
			while(!reply.equalsIgnoreCase("no"))
			{
				//System.out.println("Choose the drug and side effects you had : ");
				gui.m.jLabel2.setText("Choose the drug and side effects you had");
				DefaultListModel test1 = new DefaultListModel();

				for(int k=0;k<DList.size();k++)
				{
					if(!AllergicDrugs.contains(DList.get(k).get(0)))
						test1.addElement(DList.get(k).get(0));
				}
				gui.m.jList1.setModel(test1);
				gui.m.jList1.setEnabled(true);
				gui.m.jTextArea1.setText("");
				while(!buttonPressed)
				{
					System.out.print("");
				}
				//System.out.println("Button Pressed");
				buttonPressed=false;
				String sideeffects=gui.m.jTextArea1.getText(); /*Drug:Sideeeffects*/
				
				ArrayList<String> newSideEffects=LearningAgent(model,DList,sideeffects);
				if(newSideEffects.size()>1)
				{
						extendModel(newSideEffects);
				}
				AllergicDrugs.add(newSideEffects.get(newSideEffects.size()-1));
				gui.m.jLabel2.setText("Do you have more? ");
				gui.m.jTextArea1.setText("");
				gui.m.jList1.setEnabled(false);
				while(!buttonPressed)
				{
					System.out.print("");
				}
				//System.out.println("Button Pressed");
				buttonPressed=false;
				reply=gui.m.jTextArea1.getText(); /*Drug:Sideeeffects*/
			}
				
		}
		String msg="";
		msg+="You have "+ disease+"\n";
		msg+="Suggested Prescription :\n";
		
		for(int k=0;k<DList.size();k++)
		{
			if(!AllergicDrugs.contains(DList.get(k).get(0)))
			msg+=DList.get(k).get(0)+"\n";
		}
		
		msg+="Take Care !!!";
		JOptionPane.showMessageDialog(null,msg);
		}
		else
		{
			String msg="";
			msg+="Suggested Prescription :\n";
			//Only that symptom which is said by the user-no other symptoms are there
			ArrayList<String> druglist=issueSPARQL_Diseases_WithNoCommonSymptom(model ,symptoms);
			for(int k=0;k<druglist.size();k++)
			{
			msg+=druglist.get(k)+"\n";
			}
			extendModel_NewDisease(druglist,symptoms);
			msg+="Take Care !!!";
			JOptionPane.showMessageDialog(null,msg);
		}
	}
	
	/**
	 * Function that retrieves drugs for individual symptoms if no other disease matches with given set of symptoms
	 * @param m
	 * @param symptoms
	 * @return
	 */
	public static ArrayList<String> issueSPARQL_Diseases_WithNoCommonSymptom(Model m,List<String> symptoms)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		String CuresNameSpace="<http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#Cures>";
		
		ArrayList<String> DrugList=new ArrayList<String>();
		for(int i=0;i<symptoms.size();i++)
		{
			String ntemp="<"+defaultNameSpace+symptoms.get(i)+">";
			String queryStringDisease ="SELECT ?Drug "+
					"WHERE {  ?Drug "+CuresNameSpace+" " +ntemp + " ."  +
					"   }";
			List<String> temp=issueSPARQL_Diseases(queryStringDisease, m);
			for(int j=0;j<temp.size();j++)
			{
				DrugList.add(temp.get(j));
			}
		}
		System.out.println(DrugList);
		return DrugList;
	}
	
	/**
	 * Function that retrieves a set of diseases fromowl file given a set of symptoms
	 * @param queryString
	 * @param m
	 * @return
	 */
	public static List<String> issueSPARQL_Diseases(String queryString, Model m) 
	{
		//System.out.print(queryString);
		Query query = QueryFactory.create(queryString);
        List<Integer> startindex=new ArrayList<Integer>();
        List<Integer> endindex=new ArrayList<Integer>();
        List<String> ExtractedDiseaseList=new ArrayList<String>();

		QueryExecution qe = QueryExecutionFactory.create(query, m);
		ResultSet response = qe.execSelect();
		
		List<ResultSet> DiseaseList=ResultSetFormatter.toList(response);
			//Extracting each disease
		String Diseases=DiseaseList.toString();
		int index = Diseases.indexOf("#");
		while (index >= 0)
		{
			startindex.add(index);
		    index = Diseases.indexOf("#", index + 1);
		}
		
		int index1 = Diseases.indexOf(">");
		while (index1 >= 0)
		{
			endindex.add(index1);
		    index1 = Diseases.indexOf(">", index1 + 1);
		}
		
		for(int i=0;i<startindex.size();i++)
		{
			ExtractedDiseaseList.add(Diseases.substring(startindex.get(i)+1, endindex.get(i)));
		}
		qe.close();
		return ExtractedDiseaseList;
    }

/**
 * Given a set of diseases ,this function queries user with unique set of symptoms and choses the appropriate disease
 * @param m
 * @param Diseases
 * @return
 */
	
	public String filterDisease(Model m,ArrayList<String> Diseases)
	{
		if(Diseases.size()==1)
		{
			System.out.println("Test");
			return Diseases.get(0);
		}
		if(Diseases.size()==0)
			return null;
		Scanner sc = new Scanner(System.in);
		HashMap<String,String> symDis = new HashMap<>();
		HashSet<String> hs = new HashSet<>();
		for(String dis : Diseases)
		{
			ArrayList<String> symptoms = getSymptoms(m, dis);
			for(String sym : symptoms)
			{
				if(!hs.contains(sym))
				{
					symDis.put(sym, dis);
					hs.add(sym);
				}
				else
				{
					
					symDis.remove(sym);
				}
			}
		}
		Iterator it = symDis.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry me = (Map.Entry)it.next();
			gui.m.jLabel2.setText("Did you have "+me.getKey()+"?");
			gui.m.jTextArea1.setText("");
			System.out.println("Did you have "+me.getKey()+"?");
			while(!buttonPressed)
			{
				System.out.print("");
			}
			//System.out.println("Button Pressed");
			buttonPressed=false;
			
			if(gui.m.jTextArea1.getText().equalsIgnoreCase("yes"))
			{
				return me.getValue().toString();
			}
		
		}
		return null;
	}
	
	/**
	 * Function to get symptoms given the disease
	 * @param m
	 * @param disease
	 * @return
	 */
	public static ArrayList<String> getSymptoms(Model m,String disease)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		List<Integer> startindex=new ArrayList<Integer>();
	    List<Integer> endindex=new ArrayList<Integer>();
	    ArrayList<String> ExtractedSymptomList=new ArrayList<String>();
		String dtemp="<"+defaultNameSpace+disease+">";
		String queryStringSymptoms =
				"SELECT ?Symptom " +
						"WHERE {" +
						"     " + dtemp + " <http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#hasSymptom>  ?Symptom ." +
						"      }";
		
		Query query = QueryFactory.create(queryStringSymptoms);
		QueryExecution qe = QueryExecutionFactory.create(query, m);
		ResultSet response = qe.execSelect();
		List<ResultSet> SymptomList=ResultSetFormatter.toList(response);
		String sideeffect=SymptomList.toString();
		int index = sideeffect.indexOf("#");
		while (index >= 0)
		{
			startindex.add(index);
			index = sideeffect.indexOf("#", index + 1);
		}
		int index1 = sideeffect.indexOf(">");
		while (index1 >= 0)
		{
			endindex.add(index1);
			index1 = sideeffect.indexOf(">", index1 + 1);
		}
		
		for(int i=0;i<startindex.size();i++)
		{
			ExtractedSymptomList.add(sideeffect.substring(startindex.get(i)+1, endindex.get(i)));
		}
		qe.close();
			
		return ExtractedSymptomList;
		
	}

	/**
	 * Given a disease in query string,This methos retrieves the drugs associated with that disease
	 * @param queryString
	 * @param m
	 * @return
	 */
	public static ArrayList<String> issueSPARQLDrug(String queryString,Model m)
	{
		Query query = QueryFactory.create(queryString);
		List<Integer> startindex=new ArrayList<Integer>();
	    List<Integer> endindex=new ArrayList<Integer>();
	    ArrayList<String> ExtractedDrugList=new ArrayList<String>();
		QueryExecution qe = QueryExecutionFactory.create(query, m);
		ResultSet response = qe.execSelect();
		List<ResultSet> DrugList=ResultSetFormatter.toList(response);
		
		String Drugs=DrugList.toString();
		  
		int index = Drugs.indexOf("#");
		while (index >= 0)
		{
			startindex.add(index);
		    index = Drugs.indexOf("#", index + 1);
		}
		int index1 = Drugs.indexOf(">");
		while (index1 >= 0)
		{
		endindex.add(index1);
		index1 = Drugs.indexOf(">", index1 + 1);
		}
		for(int i=0;i<startindex.size();i++)
		{
			ExtractedDrugList.add(Drugs.substring(startindex.get(i)+1, endindex.get(i)));
		}
		qe.close();
		return ExtractedDrugList;
	}
	
	/**
	 * Given a disease this methos retrieves the side effects associated with that disease
	 * @param m
	 * @param drug
	 * @return
	 */
	public static ArrayList<String> getSideeffects(Model m,String drug)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		List<Integer> startindex=new ArrayList<Integer>();
	    List<Integer> endindex=new ArrayList<Integer>();
	    ArrayList<String> ExtractedsideeffectList=new ArrayList<String>();
		String dtemp="<"+defaultNameSpace+drug+">";
		String queryStringsideeffects =
				"SELECT ?SideEffects " +
						"WHERE {" +
						"     " + dtemp + " <http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#hasSideEffect>  ?SideEffects ." +
						"      }";
		
		Query query = QueryFactory.create(queryStringsideeffects);
		QueryExecution qe = QueryExecutionFactory.create(query, m);
		ResultSet response = qe.execSelect();
		List<ResultSet> SideeffectList=ResultSetFormatter.toList(response);
		ExtractedsideeffectList.add(drug);
				//Extracting each disease
		String sideeffect=SideeffectList.toString();
		int index = sideeffect.indexOf("#");
		while (index >= 0)
		{
			startindex.add(index);
			index = sideeffect.indexOf("#", index + 1);
		}
		int index1 = sideeffect.indexOf(">");
		while (index1 >= 0)
		{
			endindex.add(index1);
			index1 = sideeffect.indexOf(">", index1 + 1);
		}
		for(int i=0;i<startindex.size();i++)
		{
			ExtractedsideeffectList.add(sideeffect.substring(startindex.get(i)+1, endindex.get(i)));
		}
		qe.close();
		return ExtractedsideeffectList;
	}
	
	/**
	 * Adds new side effects which the system learnt from the user input
	 * @param m
	 * @param DrugAndSideeffects
	 * @param sideeffects
	 * @return
	 */
	public static ArrayList<String> LearningAgent(Model m,ArrayList<ArrayList<String>> DrugAndSideeffects,String sideeffects)
	{
		ArrayList<String> ExistingSideEffects=new ArrayList<String>();
		ArrayList<String> newSideeffects=new ArrayList<String>();
		int flag=0;
		List<String> userInput = Arrays.asList(sideeffects.split(","));
		for(int i=0;i<DrugAndSideeffects.size();i++)
		{
			if(DrugAndSideeffects.get(i).get(0).compareTo(userInput.get(0))==0)
			{
			flag=i;
			}
		}
		for(int i=1;i<DrugAndSideeffects.get(flag).size();i++)
		{
			ExistingSideEffects.add(DrugAndSideeffects.get(flag).get(i));
			
		}
		Collection<String> similar=new HashSet<String>(ExistingSideEffects);
		Collection<String> different=new HashSet<String>();
		different.addAll(userInput);
		similar.retainAll(userInput);
		different.removeAll(similar);
		different.remove(userInput.get(0));
		newSideeffects.addAll(different);
		newSideeffects.add(userInput.get(0));
		return newSideeffects;
	}
	
	/**
	 * Function to get initial set of input from the patient - symptoms
	 * @return
	 */
	public List<String> getSymptomsFromPatients()
	{
		String temp=null;
		System.out.println("Hello Patient,May I know your Concerns\n");
		gui.m.jLabel2.setText("Hello Patient,May I know your Concerns");
		gui.m.jTextArea1.setText("");
		//Scanner sc=new Scanner(System.in);
		//temp=sc.nextLine();
		//System.out.println(mf.buttonPressed);
		while(!buttonPressed)
		{
			System.out.print("");
		}
		//System.out.println("Button Pressed");
		buttonPressed=false;
		
		temp=gui.m.jTextArea1.getText();
		List<String> symptoms = Arrays.asList(temp.split(","));
		return symptoms;
	}
	
	public static void main(String args[])
	{
		OwlReader or = new OwlReader();
		Model inputModel = or.readOwlFile();
        List<String> symptoms= or.getSymptomsFromPatients();
        or.queryModel(symptoms);
    }
}


class GUIThread implements Runnable
{	
	public MainFrame m;
	public OwlReader parent;
	boolean bp;

	public GUIThread(OwlReader o)
	{
		bp = false;
		parent = o;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		m = new MainFrame(this);
		m.setVisible(true);
		m.jPanel5.setVisible(false);
		new Thread(m).start();
		while(true)
		{
			System.out.print("");
			while(!bp)
			{
				System.out.print("");
			}
			//System.out.println("Thread");
			parent.buttonPressed = true;
			bp = false;
		}
	}
	
}