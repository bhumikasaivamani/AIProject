

import java.io.*;
import java.util.*;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;


public class  OwlReader {
	String autuersBaseURI = "http://www.utdallas.edu/auteurs/";
	

	/**
	 * Function to read the OWL File
	 * @return - a Model is returned
	 */
	public Model readOwlFile()
	{
		Model model = ModelFactory.createDefaultModel();

		// use the FileManager to find the input file
		InputStream in = FileManager.get().open( "Ontology.owl" );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: not found");
		}

		// read the RDF/XML file
		model.read(in, null);
		return model;
	}
	

	public void extendModel(ArrayList<String> newSideEffects)
	{
		String defaultNameSpace = "http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#";
		Model model = null;
		InputStream in = null;

		try 
		{

			in = new FileInputStream(new File("Ontology.owl"));
		    								// Create an empty in-memory model and populate it from the graph
			model = ModelFactory.createOntologyModel();
			model.read(in,defaultNameSpace); // null base URI, since model URIs are absolute
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
			// Create an empty in-memory model and populate it from the graph
			model = ModelFactory.createOntologyModel();
			model.read(in,defaultNameSpace); // null base URI, since model URIs are absolute
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
		
		String disease = filterDisease(model, new ArrayList<>(Diseases));
	
		/****************************************Getting Drugs*********************************************************/
		
		String dtemp="<"+defaultNameSpace+disease+">";
		String queryStringDrug =
		"SELECT ?Drug " +
				"WHERE {" +
				"     " +dtemp+ " <http://www.semanticweb.org/bhumika/ontologies/2014/3/untitled-ontology-7#hasDrug>  ?Drug ." +
				//+" ?sub ?pred ?obj "+
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
				
		System.out.println("Did u have side effects with any of the drugs ?");
		Scanner sc=new Scanner(System.in);
		if(sc.nextLine().equalsIgnoreCase("yes"))
		{	
			String reply = "yes";
			while(!reply.equalsIgnoreCase("no"))
			{
				System.out.println("Enter the drug and side effects you had : ");
				String sideeffects=sc.nextLine(); /*Drug:Sideeeffects*/
				
				ArrayList<String> newSideEffects=LearningAgent(model,DList,sideeffects);
				if(newSideEffects.size()>1)
				{
						extendModel(newSideEffects);
				}
				AllergicDrugs.add(newSideEffects.get(newSideEffects.size()-1));
				System.out.println("Do you have more? ");
				reply=sc.nextLine(); /*Drug:Sideeeffects*/
			}
				
		}
		
		System.out.println("you have the Disease "+ disease);
		System.out.println("Suggested Prescription :");
		
		for(int k=0;k<DList.size();k++)
		{
			if(!AllergicDrugs.contains(DList.get(k).get(0)))
			System.out.println(DList.get(k).get(0));
		}
		sc.close();
	}
	
	public static List<String> issueSPARQL_Diseases(String queryString, Model m) 
	{
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


	
	public String filterDisease(Model m,ArrayList<String> Diseases)
	{
		if(Diseases.size()==1)
			return Diseases.get(0);
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
			System.out.println("Did you have "+me.getKey()+"?");
			if(sc.nextLine().equalsIgnoreCase("yes"))
			{
				return me.getValue().toString();
			}
		
		}
		return Diseases.get(0);
	}
	
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
	
	public List<String> getSymptomsFromPatients()
	{
		String temp=null;
		System.out.println("Hello Patient,May I know your Concerns\n");
		Scanner sc=new Scanner(System.in);
		temp=sc.nextLine();
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
