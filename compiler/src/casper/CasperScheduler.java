/* 
 * The scheduler class is used to add custom passes in the compilation
 * process. Furthermore, the exact order in which the passes must execute
 * is defined. Our additional passes execute just before the code generation
 * process and after the  code has been validated to confirm it is legal
 * Java 7.
 * 
 * List of custom passes in proper order:
 *    1) TODO: Finish this
 * 
 * Details for each compiler pass can be found in the respective visitor
 * class.
 *    
 * - Maaz
 */

package casper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import casper.visit.ExtractInputVariables;
import casper.visit.ExtractLoopCounters;
import casper.visit.ExtractLoopTerminateConditions;
import casper.visit.ExtractOperators;
import casper.visit.ExtractOutputVariables;
import casper.visit.ExtractUserDefinedDataTypes;
import casper.visit.GenerateScaffold;
import casper.visit.GenerateSparkCode;
import casper.visit.GenerateVerification;
import casper.visit.SelectLoopsForTranslation;
import polyglot.ast.NodeFactory;
import polyglot.ext.jl7.JL7Scheduler;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.EmptyGoal;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.visit.ExpressionFlattener;
import polyglot.visit.LoopNormalizer;

public class CasperScheduler extends JL7Scheduler {
	
	CasperScheduler(ExtensionInfo extInfo) {
        super(extInfo);
    }
	
	/*
	 * Built-in class. Converts all loops to standard while format.
	 * https://www.cs.cornell.edu/Projects/polyglot/api2/polyglot/visit/LoopNormalizer.html
	 */
	public Goal LoopsNormalized(final Job job){
    	TypeSystem ts = extInfo.typeSystem();
    	NodeFactory nf = extInfo.nodeFactory();
    	Goal g = internGoal(new VisitorGoal(job, new LoopNormalizer(job,ts,nf)));
            	
    	try {
    		g.addPrerequisiteGoal(TypeChecked(job),this);
    		g.addPrerequisiteGoal(ConstantsChecked(job),this);
    		g.addPrerequisiteGoal(ReachabilityChecked(job),this);
    		g.addPrerequisiteGoal(ExceptionsChecked(job),this);
    		g.addPrerequisiteGoal(ExitPathsChecked(job),this);
    		g.addPrerequisiteGoal(InitializationsChecked(job),this);
    		g.addPrerequisiteGoal(ConstructorCallsChecked(job),this);
    		g.addPrerequisiteGoal(ForwardReferencesChecked(job),this);
    		g.addPrerequisiteGoal(AnnotationsResolved(job), this);
    		g.addPrerequisiteGoal(Disambiguated(job), this);
    		g.addPrerequisiteGoal(SignaturesDisambiguated(job), this);
    		g.addPrerequisiteGoal(SupertypesDisambiguated(job), this);
    		g.addPrerequisiteGoal(TypesInitialized(job), this);
    	}
        catch (CyclicDependencyException e) { 
        	throw new InternalCompilerError(e); 
    	}
    	
        return g;
    }
	
	/*
	 * Built-in class. Converts all expressions to a set few standard formats.
	 * https://www.cs.cornell.edu/Projects/polyglot/api2/polyglot/visit/ExpressionFlattener.html
	 */
	public Goal ExpressionsFlattened(final Job job){
    	TypeSystem ts = extInfo.typeSystem();
    	NodeFactory nf = extInfo.nodeFactory();
    	Goal g = internGoal(new VisitorGoal(job, new ExpressionFlattener(job,ts,nf)));
    	
    	try {
        	g.addPrerequisiteGoal(LoopsNormalized(job), this);
    	}
        catch (CyclicDependencyException e) { 
        	throw new InternalCompilerError(e); 
    	}   
    	
        return g;
    }
	
	/*
	 * Mark loops that are suitable for an optimization attempt.
	 */
    public Goal LoopsSelected (Job job) 
    {
        Goal g = internGoal(new VisitorGoal(job, new SelectLoopsForTranslation()));
        
        try { 
        	g.addPrerequisiteGoal(ExpressionsFlattened(job), this);
    	}
        catch (CyclicDependencyException e) { 
        	throw new InternalCompilerError(e); 
    	}        

        return internGoal(g);
    }
    
    /*
     * Extract the set of loop counters in each loop fragment.
     */
    public Goal LoopCountersExtracted (Job job)
    {
    	Goal g = internGoal(new VisitorGoal(job, new ExtractLoopCounters()));
    	
    	try { 
			g.addPrerequisiteGoal(LoopsSelected(job), this); 
		}
    	catch (CyclicDependencyException e) { 
    		throw new InternalCompilerError(e); 
		}        

    	return internGoal(g);    	
    }
    
    /*
     * Extract the set of input variables to each loop fragment.
     */
    public Goal InputVariablesExtracted (Job job)
    {
    	Goal g = internGoal(new VisitorGoal(job, new ExtractInputVariables()));
    	
    	try { 
			g.addPrerequisiteGoal(LoopCountersExtracted(job), this); 
		}
    	catch (CyclicDependencyException e) { 
    		throw new InternalCompilerError(e); 
		}        

    	return internGoal(g);    	
    }
    
    /*
     * Extract the set of input variables to each loop fragment.
     */
    public Goal OutputVariablesExtracted (Job job)
    {
    	Goal g = internGoal(new VisitorGoal(job, new ExtractOutputVariables()));
    	
    	try { 
			g.addPrerequisiteGoal(InputVariablesExtracted(job), this); 
		}
    	catch (CyclicDependencyException e) { 
    		throw new InternalCompilerError(e); 
		}        

    	return internGoal(g);    	
    }
    
    /*
     * Extract the set of operators
     */
    public Goal OperatorsExtracted (Job job)
    {
    	Goal g = internGoal(new VisitorGoal(job, new ExtractOperators()));
    	
    	try { 
			g.addPrerequisiteGoal(OutputVariablesExtracted(job), this); 
		}
    	catch (CyclicDependencyException e) { 
    		throw new InternalCompilerError(e); 
		}        

    	return internGoal(g);    	
    }
    
    /*
     * Extract the condition which terminates the loop.
     */
    public Goal LoopTerminateConditionsExtracted(final Job job){
		
		Goal g = internGoal(new VisitorGoal(job, new ExtractLoopTerminateConditions()));
	        	
		try {
			g.addPrerequisiteGoal(OperatorsExtracted(job), this);
		}
	    catch (CyclicDependencyException e) { 
	    	throw new InternalCompilerError(e); 
		}   
		
	    return g;
	}
    
    /*
     * Extract user defined data types
     */
    public Goal UserDefinedDataTypesExtracted (Job job)
    {
		Goal g = internGoal(new VisitorGoal(job, new ExtractUserDefinedDataTypes()));
		
    	try {
			g.addPrerequisiteGoal(LoopTerminateConditionsExtracted(job), this);
		}
    	catch (CyclicDependencyException e) {
    		throw new InternalCompilerError(e);
		}

    	return internGoal(g);
    }
    
    /*
     * Generate verification code
     */
    public Goal VerificationCodeGenerated (Job job)
    {
		Goal g = internGoal(new VisitorGoal(job, new GenerateVerification(extInfo.nodeFactory())));
		
    	try {
			g.addPrerequisiteGoal(UserDefinedDataTypesExtracted(job), this);
		}
    	catch (CyclicDependencyException e) {
    		throw new InternalCompilerError(e);
		}

    	return internGoal(g);
    }
    
    /*
     * Generate the sketch specification document.
     */
    public Goal ScaffoldGenerated (Job job)
    {
		Goal g = internGoal(new VisitorGoal(job, new GenerateScaffold(extInfo.nodeFactory())));
		
    	try {
			g.addPrerequisiteGoal(VerificationCodeGenerated(job), this);
		}
    	catch (CyclicDependencyException e) {
    		throw new InternalCompilerError(e);
		}

    	return internGoal(g);
    }
    
    /*
     * Generate the output code
     */
    public Goal SparkCodeGenerated (Job job)
    {
		Goal g = internGoal(new VisitorGoal(job, new GenerateSparkCode(extInfo.nodeFactory())));
		
    	try {
			g.addPrerequisiteGoal(ScaffoldGenerated(job), this);
		}
    	catch (CyclicDependencyException e) {
    		throw new InternalCompilerError(e);
		}

    	return internGoal(g);
    }
	
    @Override
    public Goal Serialized (Job job) 
    {	
		Goal g = null;
		g = internGoal(new EmptyGoal(job, "empty goal")
		{
			@Override
			public Collection<Goal> prerequisiteGoals (Scheduler scheduler) 
			{
				List<Goal> l = new ArrayList<Goal>();
				l.addAll(super.prerequisiteGoals(scheduler));
				l.add(SparkCodeGenerated(job));
				return l;
			}
		});
           
        return g;
    }
}