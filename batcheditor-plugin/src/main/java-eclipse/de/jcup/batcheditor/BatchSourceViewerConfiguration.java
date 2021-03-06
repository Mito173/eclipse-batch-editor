/*
 * Copyright 2017 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.batcheditor;

import static de.jcup.batcheditor.BatchEditorUtil.*;
import static de.jcup.batcheditor.document.BatchDocumentIdentifiers.*;
import static de.jcup.batcheditor.preferences.BatchEditorSyntaxColorPreferenceConstants.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import de.jcup.batcheditor.document.BatchDocumentIdentifier;
import de.jcup.batcheditor.document.BatchDocumentIdentifiers;
import de.jcup.batcheditor.presentation.BatchDefaultTextScanner;
import de.jcup.batcheditor.presentation.PresentationSupport;
/**
 * 
 * @author Albert Tregnaghi
 *
 */
public class BatchSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private BatchDefaultTextScanner gradleScanner;
	private ColorManager colorManager;

	private TextAttribute defaultTextAttribute;
	private BatchEditorAnnotationHoover annotationHoover;
	private IAdaptable adaptable;
	private ContentAssistant contentAssistant;
	private BatchEditorSimpleWordContentAssistProcessor contentAssistProcessor;
	
	/**
	 * Creates configuration by given adaptable
	 * 
	 * @param adaptable
	 *            must provide {@link ColorManager} and {@link IFile}
	 */
	public BatchSourceViewerConfiguration(IAdaptable adaptable) {
		IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
		this.fPreferenceStore = new ChainedPreferenceStore(
				new IPreferenceStore[] { getPreferences().getPreferenceStore(), generalTextStore });

		Assert.isNotNull(adaptable, "adaptable may not be null!");
		this.annotationHoover = new BatchEditorAnnotationHoover();
		
		this.contentAssistant = new ContentAssistant();
		contentAssistProcessor = new BatchEditorSimpleWordContentAssistProcessor();
		contentAssistant.enableColoredLabels(true);
		
		contentAssistant.setContentAssistProcessor(contentAssistProcessor, IDocument.DEFAULT_CONTENT_TYPE);
		for (BatchDocumentIdentifier identifier: BatchDocumentIdentifiers.values()){
			contentAssistant.setContentAssistProcessor(contentAssistProcessor, identifier.getId());
		}
		
		contentAssistant.addCompletionListener(contentAssistProcessor.getCompletionListener());

		this.colorManager = adaptable.getAdapter(ColorManager.class);
		Assert.isNotNull(colorManager, " adaptable must support color manager");
		this.defaultTextAttribute = new TextAttribute(
				colorManager.getColor(getPreferences().getColor(COLOR_NORMAL_TEXT)));
		this.adaptable=adaptable;
	}
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		return contentAssistant;
	}
	
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		/* currently we avoid the default quick assistence parts (spell checking etc.)*/
		return null;
	}
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		/* currently we avoid the default reconciler mechanism parts (spell checking etc.)*/
		return null;
	}
	
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return annotationHoover;
	}
	
	private class BatchEditorAnnotationHoover extends DefaultAnnotationHover {
		@Override
		protected boolean isIncluded(Annotation annotation) {
			if (annotation instanceof MarkerAnnotation) {
				return true;
			}
			/* we do not support other annotations */
			return false;
		}
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		/* @formatter:off */
		return allIdsToStringArray( 
				IDocument.DEFAULT_CONTENT_TYPE);
		/* @formatter:on */
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		if (sourceViewer == null)
			return null;

		return new IHyperlinkDetector[] { new URLHyperlinkDetector(), new BatchHyperlinkDetector(adaptable) };
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		addDefaultPresentation(reconciler);

		addPresentation(reconciler, ECHO_OUTPUT.getId(), getPreferences().getColor(COLOR_ECHO_OUTPUT), SWT.ITALIC);
		
		addPresentation(reconciler, BATCH_KEYWORD.getId(), getPreferences().getColor(COLOR_BATCH_INTERNAL_KEYWORD),SWT.BOLD);

		addPresentation(reconciler, DOUBLE_STRING.getId(), getPreferences().getColor(COLOR_GSTRING),SWT.NONE);
		
		addPresentation(reconciler, COMMENT.getId(), getPreferences().getColor(COLOR_COMMENT),SWT.NONE);
		addPresentation(reconciler, PARAMETER.getId(), getPreferences().getColor(COLOR_PARAMETERS),SWT.NONE);
		addPresentation(reconciler, BATCH_COMMAND.getId(), getPreferences().getColor(COLOR_EXTERNAL_COMMAND),SWT.BOLD|SWT.NONE);
		
		
		addPresentation(reconciler, VARIABLES.getId(), getPreferences().getColor(COLOR_KNOWN_VARIABLES),SWT.NONE);
		addPresentation(reconciler, KNOWN_VARIABLES.getId(), getPreferences().getColor(COLOR_KNOWN_VARIABLES),SWT.NONE);
		
		return reconciler;
	}

	private void addDefaultPresentation(PresentationReconciler reconciler) {
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getGradleDefaultTextScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}

	private IToken createColorToken(RGB rgb) {
		Token token = new Token(new TextAttribute(colorManager.getColor(rgb)));
		return token;
	}

	private void addPresentation(PresentationReconciler reconciler, String id, RGB rgb, int style) {
		TextAttribute textAttribute = new TextAttribute(colorManager.getColor(rgb),
				defaultTextAttribute.getBackground(), style);
		PresentationSupport presentation = new PresentationSupport(textAttribute);
		reconciler.setDamager(presentation, id);
		reconciler.setRepairer(presentation, id);
	}

	private BatchDefaultTextScanner getGradleDefaultTextScanner() {
		if (gradleScanner == null) {
			gradleScanner = new BatchDefaultTextScanner(colorManager);
			updateTextScannerDefaultColorToken();
		}
		return gradleScanner;
	}

	public void updateTextScannerDefaultColorToken() {
		if (gradleScanner == null) {
			return;
		}
		RGB color = getPreferences().getColor(COLOR_NORMAL_TEXT);
		gradleScanner.setDefaultReturnToken(createColorToken(color));
	}
	

}