![SaltNPepper project](./gh-site/img/SaltNPepper_logo2010.png)
# MergingModules
This project provides a manipulator to merge corpora from different sources. This module is implemented for the linguistic converter framework Pepper (see https://u.hu-berlin.de/saltnpepper). A detailed description of the manipulator can be found in [Merger](#details1) or in [poster](http://dx.doi.org/10.5281/zenodo.15640).

Pepper is a pluggable framework to convert a variety of linguistic formats (like [TigerXML](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/TIGERSearch/doc/html/TigerXML.html), the [EXMARaLDA format](http://www.exmaralda.org/), [PAULA](http://www.sfb632.uni-potsdam.de/paula.html) etc.) into each other. Furthermore Pepper uses Salt (see https://github.com/korpling/salt), the graph-based meta model for linguistic data, which acts as an intermediate model to reduce the number of mappings to be implemented. That means converting data from a format _A_ to format _B_ consists of two steps. First the data is mapped from format _A_ to Salt and second from Salt to format _B_. This detour reduces the number of Pepper modules from _n<sup>2</sup>-n_ (in the case of a direct mapping) to _2n_ to handle a number of n formats.

![n:n mappings via SaltNPepper](./gh-site/img/puzzle.png)

In Pepper there are three different types of modules:
* importers (to map a format _A_ to a Salt model)
* manipulators (to map a Salt model to a Salt model, e.g. to add additional annotations, to rename things to merge data etc.)
* exporters (to map a Salt model to a format _B_).

For a simple Pepper workflow you need at least one importer and one exporter.

## Requirements
Since the here provided module is a plugin for Pepper, you need an instance of the Pepper framework. If you do not already have a running Pepper instance, click on the link below and download the latest stable version (not a SNAPSHOT):

> Note:
> Pepper is a Java based program, therefore you need to have at least Java 7 (JRE or JDK) on your system. You can download Java from https://www.oracle.com/java/index.html or http://openjdk.java.net/ .


## Install module
If this Pepper module is not yet contained in your Pepper distribution, you can easily install it. Just open a command line and enter one of the following program calls:

**Windows**
```
pepperStart.bat 
```

**Linux/Unix**
```
bash pepperStart.sh 
```

Then type in command *is* and the path from where to install the module:
```
pepper> update de.hu_berlin.german.korpling.saltnpepper::pepperModules-MergingModules::https://korpling.german.hu-berlin.de/maven2/
```

## Usage
To use this module in your Pepper workflow, put the following lines into the workflow description file. Note the fixed order of xml elements in the workflow description file: &lt;importer/>, &lt;manipulator/>, &lt;exporter/>. The Merger is a manipulator module, which can be addressed by one of the following alternatives.
A detailed description of the Pepper workflow can be found on the [Pepper project site](https://u.hu-berlin.de/saltnpepper). 

### a) Identify the module by name

```xml
<manipulator name="Merger"/>
```

### b) Use properties

```xml
<manipulator name="Merger">
  <property key="PROPERTY_NAME">PROPERTY_VALUE</key>
</manipulator>
```

## Contribute
Since this Pepper module is under a free license, please feel free to fork it from github and improve the module. If you even think that others can benefit from your improvements, don't hesitate to make a pull request, so that your changes can be merged.
If you have found any bugs, or have some feature request, please open an issue on github. If you need any help, please write an e-mail to saltnpepper@lists.hu-berlin.de .

## Funders
This project has been funded by the [department of corpus linguistics and morphology](https://www.linguistik.hu-berlin.de/institut/professuren/korpuslinguistik/) of the Humboldt-Universität zu Berlin and the [Sonderforschungsbereich 632](https://www.sfb632.uni-potsdam.de/en/). 

## License
  Copyright 2014 Humboldt-Universität zu Berlin.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.


# <a name="details1">Merger</a>
The Merger allows to merge an unbound number of corpora to a single corpus with Pepper. The Merger can be integrated in a Pepper workflow as a step of the manipulation phase. 

## Properties
The merging can be customized by using the properties listed in the following table. 
|name of property			|possible values		|default value|	
|---------------------------|-----------------------|-------------|
|punctuations			    |String	                |'.',',',':',';','!','?','(',')','{','}','<','>'|
|escapeMapping				|String	                ||
|copyNodes				    |true, false			|false|
|firstAsBase	            |true, false			|false|

### punctuations
Determines the punctuation characters used to be ignored for merging. The value is a comma separated list, each entry must be surrounded by a quot: 'PUNCTUATION' (, 'PUNCTUATION')* . For instance:
```xml
<property key="punctuations">'.',',',':',';','!','?','(',')','{','}','<','>'</key>
```

### escapeMapping
Determines the mapping used in normalization step, to map special characters like umlauts. This value is a comma separated list of mappings: "REPLACED_CHARACTER" : "REPLACEMENT" (, "REPLACED_CHARACTER" : "REPLACEMENT")*. For instance:
```xml
<property key="escapeMapping">"ä":"ae","ö":"oe","ü":"ue","ß":"ss"</key>
```

### copyNodes
If this property is set to 'true', the base document is always the one, which belongs to the first SCorpusGraph (the first importer in Pepper workflow description). The value either could be 'true' or 'false'. If this value is set to false, the base document is computed automically (normally the one with the largest primary text.
		
### firstAsBase
Determines if SSpan and SStructure nodes should be copied or merged. Merged means to move all annotations to the equivalent in base document. If value is true they will be copied.


## Description of the Merging process
The merging is along the document structure, but before two or more document structures can be merged to a single one, two corresponding document structures have to be identified. The identification of merging partners is based on the name and the path of a document in the corpus structure. The merging of the content is based on the textual data. Only documents having an equal textual datasource or at least one is contained in the other are mergable. Basing on the textual alignment, the mergable tokens can be computed. In the last step these tokens are the base for the merging of the rest of the graph structure like SSpan or SStructure nodes and even relations.   

## Identification of mergable documents
To give an example of the identification of merging partners for documents, imagine two corpus structures comming from different sources, one for instance from a TIGER XML document and the other one from a EXMARaLDA document. Since neither TIGER XML nor EXMARaLDA encode the corpus structure explicitly, it is taken from the folder structure, the corpus is organized in. For our example, the root folder, which is addressed by the importer is both times the folder 'myCorpus'. This folder contains two sub-folders 'subCorpus1' and 'subCorpus2'. Each folder further contains two documents, the TIGER XML or EXMARaLDA files.

1. TIGER XML 
   ```
   myCorpus
   |
   +--subCorpus1
   |  |
   |  +--document1
   |  |
   |  +--document2
   |
   +--subCorpus2
      |
      +--document3
      |
      +--document4
   ```
1. EXMARaLDA 
   ```
   myCorpus
   |
   +--subCorpus1
   |  |
   |  +--document1
   |  |
   |  +--document2
   |
   +--subCorpus2
      |
      +--document3
      |
      +--document4
   ```
The merging partners in that example would be the documents:
1. myCorpus/subCorpus1/document1 from the TIGER XML sample and myCorpus/subCorpus1/document1 from the EXMARaLDA corpus
1. myCorpus/subCorpus1/document2 from the TIGER XML sample and myCorpus/subCorpus1/document2 from the EXMARaLDA corpus
...

## Mergability of textual datasources
Two find potentially mergable textual datasources, the Merger iterates over all textual datasources of both documents, normalizes and compares them. For instance imagine the two texts:

1. This is a sample text.
1. This       IS  aSAMPLE

Both text do not differ in their content, but in their form. Therefore they are normalized, by removing whitespaces and changing the case to lower case:

1. thisisasampletext.
1. thisisasample

After normalizing, both texts are compared by String comparision. In our our case, the second text is a substring of the first text and therefore mergable (the same goes, if both texts are equal or text 1 is a subset of text 2). 
When two mergable texts are found, an offset mapping is computed, which maps the normalized text to the original text. For the second text, this will result in the following map:

|normalized |original |
|text       |text     |
|-----------|---------|
| 1  | 1   |
| 2  | 2   |
| 3  | 3   |
| 4  | 4   |
| 5  | 12  |
| 6  | 13  |
| 7  | 15  |
| 8  | 16  |
| 9  | 17  |
| 10 | 18  |
|... | ... |

With this map the offsets for tokens pointing to the textual datasource can be mapped between the two documents using the normalized text as base.

## Merging of tokens and other nodes and edges
For each token it is now possible to detect if there is a merging partner in the other document or not with the use of the computed offsets. In case, there is no merging partner, the token is copied. In case there is a merging partner, all annotations on the token are copied. Since Salt is a graph based model, each linguistic annotation and structure is modeled as either a node an edge or a label. That means, basing on tokens, the graphs can be traversed in a bottom up like traversal to visit each node and each edge. To be more precise we give an example. Imagine the two following document structures:

1. document structure 1
   ```
         a
     /   |   \
   tok1 tok2 tok3
   ```
1. document structure 2
   ```
         b
     /   |   \
   tokA tokB tokC
   ```

We assume, that the tokens *tok1* and *tokA*, *tok2* and *tokB*, *tok3* and *tokC* are mergable. When node *a* is reached by the traversal, all child nodes of *a* will be collected. For each node in this collection the corresponding one of document structure 2 is put into another collection (containing *tokA*, *tokB* and *tokC*). Starting with this collection, a node in document structure 2 is searched, which is a parent node of all of them. This leads to node *b*. And finally node *b* is merged with node *a*. 
This process is continued until each node was merged (if it is mergable). 
   