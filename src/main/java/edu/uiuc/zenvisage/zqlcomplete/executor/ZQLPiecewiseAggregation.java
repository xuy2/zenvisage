/**
 * 
 */
package org.vde.zql;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.vde.database.refactor.ColumnMetadata;
import org.vde.database.refactor.Database;

import api.Args;
import api.Point;
import api.SketchPoints;
import normalization.Normalization;

/**
 * @author xiaofo
 *
 */
public class ZQLPiecewiseAggregation {
	public Normalization normalization;
	public Database inMemoryDatabase;
	
	/**
	 * @param normalization
	 * @param args
	 * @param inMemoryDatabase
	 */
	public ZQLPiecewiseAggregation(Normalization normalization,
			Database inMemoryDatabase) {
		this.normalization = normalization;
		this.inMemoryDatabase = inMemoryDatabase;
	}

	public double[] applyPAA(Set<Integer> ignore) {
		return null;
	}
	
	public void setPAAwidth(LinkedHashMap<String, LinkedHashMap<Float, Float>> output, String x) {
		if (output == null) return;
		ColumnMetadata columnMetadata = inMemoryDatabase.getColumnMetaData(x);
		columnMetadata.pAAWidth = (float) 1/(output.values().iterator().next().size() - 2);
		columnMetadata.numberOfSegments = output.values().iterator().next().size();
	}
	
	public double[][] applyPAAonData(LinkedHashMap<String,LinkedHashMap<Float,Float>> data, Set<Float> ignore, String x){
		double[][] normalizedgroups = new double[data.size()][];
		int count = 0;
		ColumnMetadata columnMetadata = inMemoryDatabase.getColumnMetaData(x);
		float pAAWidth = columnMetadata.pAAWidth;
		int numberofsegments = (int) (1/pAAWidth);
		float min = columnMetadata.min;
		float max = columnMetadata.max;	
		float range = max-min;
	
	  
		for (String key : data.keySet()) {
			Map<Float,Float> values = data.get(key);
			double [] normalizedValues = new double[numberofsegments+1];
			int[] numberofpoints = new int[numberofsegments+1];
			for (int i = 0; i < numberofsegments; i++) {
				normalizedValues[i] = 0.0;
				numberofpoints[i] = 0;
				ignore.add((float)i);
			}
		  
			for (Float key1 : values.keySet()) {
				if (range == 0)
					continue;
				int segment = (int) (((key1-min))/(range*pAAWidth));
				normalizedValues[segment] = normalizedValues[segment] + values.get(key1);
				numberofpoints[segment] = numberofpoints[segment]+1;
				ignore.remove(key1);
			}
		  
			for (int i = 0; i <= numberofsegments; i++) {
				if (numberofpoints[i] > 0)
					normalizedValues[i] = normalizedValues[i] / numberofpoints[i];
			}
		  
			for (int i = 0; i < numberofsegments; i++) {
				if (numberofpoints[i] == 0) {
					if (i > 0 && i < numberofsegments) {
						normalizedValues[i] = (normalizedValues[i-1] + normalizedValues[i+1])/2;
					}
					else if(i > 0) {
						normalizedValues[i] = normalizedValues[i-1];
					}
					else{
						normalizedValues[i] = normalizedValues[i+1];
					}
				}
				normalization.normalize(normalizedValues);
			}
			  
			  
			normalizedgroups[count] = normalizedValues;
			count++;		  
		}
		return normalizedgroups;  
	}
	
	
	
	
	
	public double[] applyPAAonQuery(Set<Float> ignore, SketchPoints sketchPoint){
		
		ColumnMetadata xcolumnMetadata = inMemoryDatabase.getColumnMetaData(sketchPoint.xAxis);
		float pAAWidth = xcolumnMetadata.pAAWidth;
		int numberofsegments = (int) (1/pAAWidth);
		float min = sketchPoint.minX;
		float max = sketchPoint.maxX;		
		float rangeX = max-min;
		double [] normalizedValues = new double[numberofsegments+1];
		int[] numberofpoints = new int[numberofsegments+1];
		for (int i = 0;i < numberofsegments;i++) {
			normalizedValues[i] = 0.0;
			numberofpoints[i] = 0;
		}
	  
		ColumnMetadata ycolumnMetadata = inMemoryDatabase.getColumnMetaData(sketchPoint.yAxis);
		float minY = ycolumnMetadata.min;
		float maxY = ycolumnMetadata.max;
		float rangeY = maxY-minY;
	  
		float minYQ = sketchPoint.minY;
		float maxYQ = sketchPoint.maxY;		
		float rangeYQ = maxYQ-minYQ;
		for (Point p : sketchPoint.points) {		  
			int segment = (int) ((p.getX()-min)/(rangeX*pAAWidth));
			//System.out.println(segment);
			float yvalue = minY+((p.getY()-minYQ)*rangeY/(rangeYQ));
			normalizedValues[segment] = normalizedValues[segment]+yvalue;
			numberofpoints[segment] = numberofpoints[segment]+1;
		}
		  
		for (int i = 0; i <= numberofsegments; i++) {
			if(numberofpoints[i] > 0)
				normalizedValues[i] = normalizedValues[i]/numberofpoints[i];
		}
		  
		for (int i = 0; i < numberofsegments; i++) {
			if (numberofpoints[i] == 0) {
				if (i > 0 && i < numberofsegments) {
					normalizedValues[i] = (normalizedValues[i-1] + normalizedValues[i+1])/2;
				}
				else if (i > 0) {
					normalizedValues[i] = normalizedValues[i-1];
				}
				else {
					normalizedValues[i] = normalizedValues[i+1];
				}
			}
			
			if (ignore.contains(i))
				normalizedValues[i] = 0;
				
		}
		normalization.normalize(normalizedValues); 
		
		//System.out.println(Arrays.toString(normalizedValues));
		return normalizedValues;
	}
	
	
	
}
