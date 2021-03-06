package com.dianping.kernel.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bin.miao
 *
 */
public class SortTool {
	
	private static final String ruleDelimiter = "|";
	private static final String beforeOperator = "-";
	private static final String afterOperator = "+";
	private static final String nameFlag = ":";
	
	private static final int getElementByName = 1;
	private static final int getElementByClassName = 2;
	
	private boolean discardNotMatch = true;
	
	public SortTool(){
		this(true);
	}
	
	public SortTool(boolean discardNotMatch){
		this.discardNotMatch = discardNotMatch;
	}
	
	public List<SortElement> sort(List<? extends SortElement> elementList){
		
		List<SortElementWrapper> allWrapperList = new ArrayList<SortElementWrapper>();
		List<SortElementWrapper> beforeWrapperList = new ArrayList<SortElementWrapper>();
		List<SortElementWrapper> duringWrapperList = new ArrayList<SortElementWrapper>();
		List<SortElementWrapper> afterWrapperList = new ArrayList<SortElementWrapper>();
		List<SortElementWrapper> uncertainWrapperList = new ArrayList<SortElementWrapper>();
		Set<SortElementWrapper> notMatchWrapperSet = new HashSet<SortElementWrapper>();
		
		if(elementList != null && elementList.size() > 0){
			
			for(int i=0;i<elementList.size();i++){
				SortElement element = elementList.get(i);
				SortElementWrapper wrapper = new SortElementWrapper(i,element);
				allWrapperList.add(wrapper);
			}
			
			for(SortElementWrapper wrapper : allWrapperList){
				segment(wrapper.getElement().getRule(),
						wrapper,
						allWrapperList,
						beforeWrapperList,
						duringWrapperList,
						afterWrapperList,
						uncertainWrapperList,
						notMatchWrapperSet);
			}
			for(SortElementWrapper uncertainWrapper : uncertainWrapperList){
				String r = uncertainWrapper.getRuleStr();
				SortElementWrapper sew = matchRule(r,allWrapperList);
				if(sew != null && duringWrapperList.contains(sew)){
					if(r.startsWith(beforeOperator)){
						sew.addBefore(uncertainWrapper);
					}else if(r.startsWith(afterOperator)){
						sew.addAfter(uncertainWrapper);
					}else{
						notMatchWrapperSet.add(uncertainWrapper);
					}
				}else{
					notMatchWrapperSet.add(uncertainWrapper);
				}
			}
			Collections.sort(beforeWrapperList,new BeforeOrAfterComparator());
			Collections.sort(afterWrapperList,new BeforeOrAfterComparator());
			return merge(beforeWrapperList,duringWrapperList,afterWrapperList,notMatchWrapperSet);
		}
		return null;
	}
	
	private List<SortElement> merge(List<SortElementWrapper> beforeWrapperList,
			List<SortElementWrapper> duringWrapperList,
			List<SortElementWrapper> afterWrapperList,
			Set<SortElementWrapper> notMatchWrapperSet){
		List<SortElement> resultList = new ArrayList<SortElement>();
		for(SortElementWrapper sew : beforeWrapperList){
			resultList.add(sew.getElement());
		}
		for(SortElementWrapper sew : duringWrapperList){
			mergeBeforeSortElement(resultList,sew);
			resultList.add(sew.getElement());
			mergeAfterSortElement(resultList,sew);
		}
		if(!this.discardNotMatch){
			for(SortElementWrapper sew : notMatchWrapperSet){
				resultList.add(sew.getElement());
			}
		}
		for(SortElementWrapper sew : afterWrapperList){
			resultList.add(sew.getElement());
		}
		return resultList;
	}
	
	private void mergeBeforeSortElement(List<SortElement> resultList,
			SortElementWrapper sew){
		SortElementWrapper beforeSew = sew.getBefore();
		if(beforeSew != null){
			resultList.add(beforeSew.getElement());
			mergeBeforeSortElement(resultList,beforeSew);
		}
	}
	
	private void mergeAfterSortElement(List<SortElement> resultList,
			SortElementWrapper sew){
		SortElementWrapper afterSew = sew.getAfter();
		if(afterSew != null){
			resultList.add(afterSew.getElement());
			mergeAfterSortElement(resultList,afterSew);
		}
	}
	
	private void segment(String rule_,SortElementWrapper wrapper,
			List<SortElementWrapper> allWrapperList,
			List<SortElementWrapper> beforeWrapperList,
			List<SortElementWrapper> duringWrapperList,
			List<SortElementWrapper> afterWrapperList,
			List<SortElementWrapper> uncertainWrapperList,
			Set<SortElementWrapper> notMatchWrapperSet){
		String rule = rule_;
		if(rule != null){
			rule = rule_.trim();
		}
		if(rule == null || rule.length() == 0 ||rule.equals("0")){
			//init-param of "_INDEX_" is null/""/"0"
			duringWrapperList.add(wrapper);
		}else{
			Integer position = formatNumber(rule);
			if(position == null){
				//Not just numbers rule
				if(rule.indexOf(ruleDelimiter) > -1){
					String r = getEffectiveRule(rule,allWrapperList);
					if(r != null){
						segment(r,wrapper,
								allWrapperList,
								beforeWrapperList,
								duringWrapperList,
								afterWrapperList,
								uncertainWrapperList,
								notMatchWrapperSet);
					}else{
						notMatchWrapperSet.add(wrapper);
					}
					
				}else{
					uncertainWrapperList.add(wrapper);
					wrapper.setRule(rule);
				}
			}else{
				if(position > 0){
					//rule : "+1/2/3..."
					beforeWrapperList.add(wrapper);
				}else if(position < 0){
					//rule : "-1/2/3..."
					afterWrapperList.add(wrapper);
				}
				wrapper.setRule(position);
			}
		}
	}
	
	private String getEffectiveRule(String rule_,
			List<SortElementWrapper> allWrapperList){
		String rule = rule_;
		List<String> rules = new ArrayList<String>();
		int idx = rule.indexOf(ruleDelimiter);
		while(idx > -1){
			rules.add(rule.substring(0,idx));
			rule = rule.substring(idx+1);
			idx = rule.indexOf(ruleDelimiter);
		}
		rules.add(rule);
		for(int i=0;i<rules.size();i++){
			String r = rules.get(i);
			if(r != null&&r.length() > 0){
				//r : "-/+  1/2/3..."
				Integer pos = formatNumber(r);
				if(pos != null){
					return r;
				}else{
					// r : "+/- : name/classname"
					if(matchRule(r,allWrapperList) != null){
						return r;
					}
				}
			}
		}
		return null;
	}
	
	private Integer formatNumber(String r){
		String r_ = r.trim();
		if(r_.indexOf(afterOperator) == 0 && r_.length() == 2){
			r_ = r_.substring(1);
			try{
				return Integer.parseInt(r_);
			}catch(Exception e1){
				return null;
			}
		}
		
		if(r_.indexOf(beforeOperator) == 0 && r_.length() == 2){
			try{
				return Integer.parseInt(r_);
			}catch(Exception e1){
				return null;
			}
		}
		return null;
	}
	
	private SortElementWrapper matchRule(String r,List<SortElementWrapper> allWrapperList){
		if(r.indexOf(nameFlag) > 0){
			//r : "+/-:name"
			String[] params = r.split(nameFlag);
			return getElementWrapper(params[1],allWrapperList,getElementByName);
		}else{
			String className = r.substring(1);
			return getElementWrapper(className,allWrapperList,getElementByClassName);
		}
	}
	
	private SortElementWrapper getElementWrapper(String param,
			List<SortElementWrapper> allWrapperList,
			int getElementBy){
		
		for(SortElementWrapper wrapper : allWrapperList){
			if(getElementByName == getElementBy 
					&& param.equals(wrapper.getElement().getName())
					//Relative position can only act on the App configuration
					&& wrapper.getElement().getRule() == null){
				return wrapper;
			}
			if(getElementByClassName == getElementBy 
					&& param.equals(wrapper.getElement().getClassName())
					//Relative position can only act on the App configuration
					&& wrapper.getElement().getRule() == null){
				return wrapper;
			}
		}
		return null;
	}
	
	public boolean isDiscardNotMatch() {
		return discardNotMatch;
	}

	public void setDiscardNotMatch(boolean discardNotMatch) {
		this.discardNotMatch = discardNotMatch;
	}

	/**
	 * @author bin.miao
	 *
	 */
	public interface SortElement{
		public String getRule();
		public String getName();
		public String getClassName();
	}
	
	/**
	 * 
	 * @author bin.miao
	 *
	 */
	private class SortElementWrapper{
		
		private int idx = 0;
		private SortElement element;
		
		private String ruleStr = null;
		private int ruleNum = 0;
		
		private SortElementWrapper before;
		private SortElementWrapper after;
		
		SortElementWrapper(int idx,SortElement element){
			this.idx = idx;
			this.element = element;
		}
		
		public void addBefore(SortElementWrapper sew){
			if(this.before == null){
				this.before = sew;
			}else{
				if(sew.getIdx() < this.before.getIdx()){
					SortElementWrapper oldBefore = this.before;
					this.before = sew;
					this.before.addBefore(oldBefore);
				}else{
					this.before.addBefore(sew);
				}
			}
		}
		
		public void addAfter(SortElementWrapper sew){
			if(this.after == null){
				this.after = sew;
			}else{
				if(sew.getIdx() < this.after.getIdx()){
					SortElementWrapper oldAfter = this.after;
					this.after = sew;
					this.after.addAfter(oldAfter);
				}else{
					this.after.addAfter(sew);
				}
			}
		}
		
		public void setRule(int ruleNum){
			this.ruleNum = ruleNum;
		}
		
		public int getRuleNum(){
			return this.ruleNum;
		}
		
		public void setRule(String ruleStr){
			this.ruleStr = ruleStr;
		}
		
		public String getRuleStr(){
			return this.ruleStr;
		}
		
		public int getIdx() {
			return idx;
		}

		public SortElement getElement() {
			return element;
		}

		public SortElementWrapper getBefore() {
			return before;
		}

		public SortElementWrapper getAfter() {
			return after;
		}
		
	}
	
	private class BeforeOrAfterComparator implements Comparator<SortElementWrapper>{

		@Override
		public int compare(SortElementWrapper element1, SortElementWrapper element2) {
			int result = 0;
			result = element1.getRuleNum() - element2.getRuleNum();
			if(result == 0){
				result = element1.getIdx() - element2.getIdx();
			}
			return result;
		}
		
	}

}
