<!--Main page, styled using MainPage.css !-->
<template>
   <div>
    <b-navbar toggleable="lg" type="dark" variant="info">
      <b-navbar-brand href="#">ArcheGEO</b-navbar-brand>
    </b-navbar>
    <b-row class="ml-1 mt-1 mr-1">
      <b-col>
        <h2>Search GEO Repository</h2>
        <b-form @submit="onSubmit" @reset="onReset" v-if="show">
          <b-form-group id="input-group-keyword"
            label="Enter search keywords (separated by comma):"
            label-for="input-keyword" v-if="show">
            <b-form-input id="input-keyword" v-model="form.keyword"
              type="text" placeholder="Enter keywords" required></b-form-input>
          </b-form-group>
          <b-button class="mr-1" type="submit" variant="primary" :disabled="disableSubmit">Submit</b-button>
          <b-button class="mr-1" type="reset" variant="danger" :disabled="disableSubmit">Reset</b-button>
          <div class="loading-screen" v-show="loading" v-bind:class="classes" v-bind:style="{backgroundColor:bc}">
            <component v-if="customLoader" v-bind:is="customLoader"></component>
            <div v-else>
              <div class="loading-circle"></div>
              <p class="loading-text">{{busytext}}</p>
            </div>
          </div>
        </b-form>
      </b-col>
      <b-col>
        <h2>Summary of Search</h2>
        <label class="summary_label" for="display-organism-label"><b>Organism: </b></label>
        <label for="display-organism-value">{{ form.organism }}</label>
        <br>
        <label class="summary_label" for="display-disease-label"><b>Disease: </b></label>
        <label for="display-disease-value">{{ form.disease }}</label>
        <br>
        <label class="summary_label" for="display-anatomy-label"><b>Anatomy of disease: </b></label>
        <label for="display-anatomy-value">{{ form.anatomy }}</label>
        <br>
        <label class="summary_label_error" for="information" v-if="show_information"><b>Please resubmit your search. </b>{{ form.information }}</label>
      </b-col>
      <b-col class="mt-1">
        <b-button v-b-toggle.collapse-1-inner size="sm">Toggle Search Details</b-button>
          <b-collapse visible id="collapse-1-inner" class="mt-2">
            <b-card class="pre-formatted">{{ gdssearch.message }}</b-card>
          </b-collapse>
      </b-col>  
    </b-row>
    <b-row class="ml-3 mt-3 mr-1">
      <b-card no-body class="tab_card">
        <b-tabs card v-model="tabIndex">
          <b-tab v-if="showInvalid" :key="0" :title="'Invalid Records'" @click="computeInvalidRows">
            <b-card-text>
              <b-row>
                <b-col sm="5" md="6" class="my-1">
                  <b-form-group label="Per page" label-for="per-page-select" label-cols-sm="6" label-cols-md="4" label-cols-lg="3"
                    label-align-sm="right" label-size="sm" class="mb-0">
                    <b-form-select id="per-page-select" v-model="perPage" :options="pageOptions" size="sm"></b-form-select>
                  </b-form-group>
                </b-col>
                <b-col sm="7" md="6" class="my-1">
                  <b-pagination v-model="currentPage" :total-rows="totalRows" aria-controls="gdsTableInvalid" :per-page="perPage" align="fill" size="sm" class="my-0"></b-pagination>
                </b-col>
              </b-row>  
              <div class="loading-screen" v-show="loading" v-bind:class="classes" v-bind:style="{backgroundColor:bcFull}">
                <component v-if="customLoader" v-bind:is="customLoader"></component>
                  <div v-else>
                  <div class="loading-circle"></div>
                  <p class="loading-text">{{busytext}}</p>
                </div>
              </div>
              <b-table id="gdsTableInvalid" :items="invalidItems" :fields="invalidItems_fields" :select-mode="selectMode" responsive="sm" ref="selectableInvalidTable" selectable @row-selected="onInvalidRowSelected" :current-page="currentPage"
                :per-page="perPage">
                <template #cell(invalidSelected)="{ rowSelected }">
                  <template v-if="rowSelected">
                    <span aria-hidden="true">&check;</span>
                    <span class="sr-only">Selected</span>
                  </template>
                  <template v-else>
                    <span aria-hidden="true">&nbsp;</span>
                    <span class="sr-only">Not selected</span>
                  </template>
                </template>
              </b-table>
              <p>
                <b-button class="mr-1" size="sm" @click="selectAllInvalidRows">Select all</b-button>
                <b-button class="mr-1" size="sm" @click="clearInvalidSelected">Clear selected</b-button>
                <b-button class="mr-1" size="sm" @click="setAsValidRows(invalidSelected, $event)" variant="danger">Set as valid</b-button>
              </p>
              <p>
                <b-card class="pre-formatted">
                  Message:<br>
                  <span v-bind:class="{'text-info':isInfo, 'text-danger':isDanger}">{{ systemmsg }}</span><br>
                  <b-button v-if="confirmSetAsValid" class="mr-1" size="sm" @click="confirmSetAsValidRows(invalidSelected, $event)" variant="danger">Confirm</b-button>
                </b-card>
              </p>
              <!--p>
                Selected Invalid Rows:<br>
                {{ invalidSelected }}
              </p-->
            </b-card-text>  
          </b-tab>
          <b-tab v-for="(value, index) in gdssearch.tabheader" :key="value" :title="`${value}`" @click="computeRows(`${index}`)">
            <b-card-text>
              <b-row>
                <b-col sm="5" md="6" class="my-1">
                  <b-form-group label="Per page" label-for="per-page-select" label-cols-sm="6" label-cols-md="4" label-cols-lg="3"
                    label-align-sm="right" label-size="sm" class="mb-0">
                    <b-form-select id="per-page-select" v-model="perPage" :options="pageOptions" size="sm"></b-form-select>
                  </b-form-group>
                </b-col>
                <b-col sm="7" md="6" class="my-1">
                  <b-pagination v-model="currentPage" :total-rows="totalRows" aria-controls="gdsTable" :per-page="perPage" align="fill" size="sm" class="my-0"></b-pagination>
                </b-col>
              </b-row>  
              <div class="loading-screen" v-show="loading" v-bind:class="classes" v-bind:style="{backgroundColor:bcFull}">
                <component v-if="customLoader" v-bind:is="customLoader"></component>
                  <div v-else>
                  <div class="loading-circle"></div>
                  <p class="loading-text">{{busytext}}</p>
                </div>
              </div>
              <b-table id="gdsTable" :items="validItems[`${index}`]" :fields="tabFields" :select-mode="selectMode" responsive="sm" ref="selectableTable" selectable @row-selected="onRowSelected(`${index}`, $event)" :current-page="currentPage"
                :per-page="perPage">
                <template #cell(selected)="{ rowSelected }">
                  <template v-if="rowSelected">
                    <span aria-hidden="true">&check;</span>
                    <span class="sr-only">Selected</span>
                  </template>
                  <template v-else>
                    <span aria-hidden="true">&nbsp;</span>
                    <span class="sr-only">Not selected</span>
                  </template>
                </template>
                <template v-if="!isCombinedResult" #cell(validity)="row">
                  <b-form-select v-model="row.item.validity" :options="validity_options"></b-form-select>
                </template>
              </b-table>
              <p>
                <b-button class="mr-1" size="sm" @click="selectAllRows(`${index}`)">Select all</b-button>
                <b-button class="mr-1" size="sm" @click="clearSelected(`${index}`)">Clear selected</b-button>
                <b-button class="mr-1" size="sm" @click="viewInvalid" variant="primary">View invalid records</b-button>
                <b-button class="mr-1" size="sm" @click="download(selected[`${index}`], `${value}`, $event)" variant="primary">Download SOFT</b-button>
                <b-button v-if="!isCombinedResult" class="mr-1" size="sm" @click="setAsInvalidRows(selected, $event)" variant="danger">Set as invalid records</b-button>
                <b-button class="mr-1" size="sm" @click="savelist(validItems[`${index}`], `${value}`, $event)" variant="success">Save results</b-button>
              </p>
              <p>
                <b-card class="pre-formatted">
                  Message:<br>
                  <span v-bind:class="{'text-info':isInfo, 'text-danger':isDanger}">{{ systemmsg }}</span><br>
                  <b-button v-if="confirmSetAsInvalid" class="mr-1" size="sm" @click="confirmSetAsInvalidRows(selected[`${index}`], validItems[`${index}`], `${index}`, gdssearch.tabheader.length, $event)" variant="danger">Confirm</b-button>
                </b-card>
              </p>
              <!--p>
                Selected Rows:<br>
                {{ selected[`${index}`] }}
              </p-->
            </b-card-text>  
            <b-button size="sm" variant="danger" class="float-right" @click="closeTab(`${value}`)" v-if="isCombinedResult">
              Close tab
            </b-button>
          </b-tab>
          <!-- New Tab Button (Using tabs-end slot) -->
          <template #tabs-end>
            <b-nav-item v-if="hasResults" v-b-modal.modal-resultCombi role="presentation" href="#"><b>+</b></b-nav-item>
            <b-modal size="lg" ref="modalResultCombi" id="modal-resultCombi" title="Select Result Combination" ok-title="Create View Tab" 
              @ok="createNewTab" @hidden="clearDialog" @cancel="clearDialog">
                <b-form ref="form-newtab" @ok="createNewTab">
                  <b-form-group id="input-group-tabname">
                    <b-row class="my-1">
                      <b-col sm="3">
                        <label for="input-tabname">Tab Name:</label>
                      </b-col>
                      <b-col sm="8">
                        <b-form-input id="input-tabname" v-model="ruleForm.tabname" :state="ruleForm.tabnameState"
                          type="text" @change="validateTabname"></b-form-input>
                        <b-form-invalid-feedback :state="ruleForm.tabnameState">
                          {{tabnameError}}
                        </b-form-invalid-feedback>
                      </b-col>
                    </b-row>
                    <b-row v-if="ruleForm.showDialogError">
                      <b-col>
                        <span style="color:red"><b-icon icon="exclamation-triangle"></b-icon>{{ruleForm.dialogError}}</span>
                      </b-col>
                    </b-row>
                  </b-form-group>
                  <div>
                    <b-container class="bv-example-row">
                      <b-row>
                        <b-col>
                          <b-table sticky-header :items="ruleForm.rulelist" :fields="visibleFields" striped responsive="sm" thead-class="d-none">
                            <template v-slot:cell(movement)="row">
                              <b-button variant=primary size="sm" class="mr-2" @click="moveRuleUp(row.index)">
                                <b-icon icon="arrow-up"></b-icon>
                              </b-button>
                              <b-button variant=danger size="sm" class="mr-2" @click="moveRuleDown(row.index)">
                                <b-icon icon="arrow-down"></b-icon>
                              </b-button>
                            </template>
                            <template sm="10" v-slot:cell(resultTabDisplay)="row">
                              <span v-if="row.item.negation" style="color:red">!(</span>
                              {{ row.item.resultTabDisplay }}
                              <span v-if="row.item.negation" style="color:red">)</span>
                            </template>
                            <template sm="1" v-slot:cell(operator)="row">
                              <b-form-select v-if="row.item.showOperator" v-model="row.item.operator" 
                              :options="operatorOptions" :state="row.item.operatorState" @change="getCurrOperatorState(row.item.operator, row.index)"></b-form-select>
                            </template>
                            <template sm="1" v-slot:cell(delete)="row">
                              <b-button size="sm" @click="deleteRule(row.index)" class="mr-2">
                                <b-icon icon="trash"></b-icon>
                              </b-button>
                              <b-button v-b-modal.modal-editRule @click="editRule(row.index)" size="sm" class="mr-2">
                                <b-icon icon="pencil"></b-icon>
                              </b-button>
                              <b-modal v-if="row.item.edit" size="lg" ref="modalEditRule" id="modal-editRule" title="Edit Rule" ok-title="Confirm" 
                                @ok="confirmEditRule" @cancel="clearEditRule" @hidden="clearEditRule">
                                <b-form ref="form-editRule">
                                  <b-form-group id="input-group-currResultTab">
                                    <b-row class="my-1">
                                      <b-col sm="3">
                                        <label for="input-currResultTab">Result tab:</label>
                                      </b-col>
                                      <b-col sm="8">
                                        <span v-if="row.item.negation" style="color:red">!(</span>
                                        {{ row.item.resultTabDisplay }}
                                        <span v-if="row.item.negation" style="color:red">)</span>
                                      </b-col>
                                    </b-row>
                                    <b-row class="my-1">
                                      <b-col sm="3">
                                        <label for="input-currResultTab">New result tab:</label>
                                      </b-col>
                                      <b-col sm="2">
                                        <b-form-checkbox id="checkbox-not-editrule" v-model="ruleForm.currNegation" name="checkbox-editrule">
                                          Negate
                                        </b-form-checkbox>
                                      </b-col>
                                      <b-col sm="6">
                                        <b-form-select v-model="ruleForm.currResultTab" :options="resultTabOptions" :state="ruleForm.currResultTabState"></b-form-select>
                                      </b-col>
                                    </b-row>
                                  </b-form-group>
                                </b-form>
                              </b-modal>
                            </template>
                          </b-table>
                        </b-col>
                      </b-row>
                    </b-container>
                  </div>
                  <div>
                    <hr class="style2">
                    <b-form ref="form-newRule">
                      <b-form-group id="input-group-rule">
                        <b-row class="my-1">
                          <b-col sm="1">
                          </b-col>
                          <b-col sm="3">
                            <b-button variant=success size="sm" @click="addRule" class="mr-2">
                              + New Rule
                            </b-button>
                          </b-col>
                          <b-col sm="2">
                            <b-form-checkbox id="checkbox-not" v-model="ruleForm.currNegation" name="checkbox-not">
                              Negate
                            </b-form-checkbox>
                          </b-col>
                          <b-col sm="5">
                            <b-form-select v-model="ruleForm.currResultTab" :options="resultTabOptions" 
                              :state="ruleForm.currResultTabState" @change="validateResultTab"></b-form-select>
                          </b-col>
                        </b-row>
                      </b-form-group>
                    </b-form>
                  </div>
                </b-form>
            </b-modal>
          </template>
        </b-tabs>
      </b-card>
    </b-row>
  </div>
</template>

<script>
import api from './backend-api'
import XLSX from 'xlsx';

export default {
  name: 'search',
  data() {
    return {
      //initialization
      disableSubmit: true,
      //for loading screen
      busytext: 'Loading...',
      dark: false,
      classes: null,
      loading: false,
      background: null,
      customLoader: null,
      //************************************ 
      backendResponse: [],//1st element is TRUE (FALSE) if successful (unsuccessful), 2nd element is organism, 3rd element is disease, 4th element is anatomy 
      form: {
        keyword: '',
        organism: '',
        disease: '',
        anatomy: '',
        information: '',
      },
      gdssearch: {
        message: '',
        gdssize: '',
        tabheader: [],
        organismList: '',
        diseaseList: '',
      },
      tabIndex: 1,
      validItems: [],//gdsid, organism, platform, samplenum
      totalRows: 1,
      totalValidTabs: 0,
      validItems_fields: [
        { key: 'selected', label: 'Selected', sortable: false},
        { key: 'gdsid', label: 'GDS_ID', sortable: true},
        { key: 'organism', label: 'Organism', sortable: true},
        { key: 'platform', label: 'Platform', sortable: true},
        { key: 'samplenum', label: 'Sample Num', sortable: true},
        { key: 'validity', label: 'Validity', sortable: true},
      ],
      validItems_fields_combined: [
        { key: 'selected', label: 'Selected', sortable: false},
        { key: 'gdsid', label: 'GDS_ID', sortable: true},
        { key: 'platform', label: 'Platform', sortable: true},
        { key: 'samplenum', label: 'Sample Num', sortable: true},
        { key: 'inputorganism', label: 'Input Organism', sortable: true},
        { key: 'inputdisease', label: 'Input Disease', sortable: true},
      ],
      tabFields: [],
      selected: [],
      show: true,
      show_information: false,
      showInvalid: false,
      invalidItems: [],//gdsid, observedorganism, inputorganism, inputdisease, invalidreason
      invalidItems_fields: [
        { key: 'invalidSelected', label: 'Selected', sortable: false},
        { key: 'gdsid', label: 'GDS_ID', sortable: true},
        { key: 'observedorganism', label: 'Organism', sortable: true},
        { key: 'inputorganism', label: 'Input Organism', sortable: true},
        { key: 'inputdisease', label: 'Input Disease', sortable: true},
        { key: 'invalidreason', label: 'Invalid Reason', sortable: true},
      ],
      invalidSelected: [],
      selectMode: 'multi',
      currentPage: 1,
      perPage: 10,
      //pageOptions: [10, 50, { value: 100, text: "Show a lot" }],
      pageOptions: [10, 50, 100],
      systemmsg: '',
      isDanger: false,
      isInfo: true,
      confirmSetAsValid: false,
      validity_options: [
        { value: "Valid", text: 'Valid' },
        { value: "Organism Mismatch, Disease Valid", text: 'Organism Mismatch, Disease Valid' },
        { value: "Organism Valid, Disease Mismatch", text: 'Organism Valid, Disease Mismatch' },
        { value: "Organism and Disease Mismatch", text: 'Organism & Disease Mismatch' },
        { value: "Organism Mismatch, Disease Unverified", text: 'Organism Mismatch, Disease Unverified' },
        { value: "Organism Valid, Disease Unverified", text: 'Organism Valid, Disease Unverified' },
        { value: "Organism Unverified, Disease Valid", text: 'Organism Unverified, Disease Valid' },
        { value: "Organism Unverified, Disease Unverified", text: 'Organism Unverified, Disease Unverified' },
        { value: "Organism Unverified, Disease Mismatch", text: 'Organism Unverified, Disease Mismatch' },
      ],
      confirmSetAsInvalid: false,
      /***combined result tab */
      isCombinedResult: false,
      combinedResultTabList: [],
      hasResults: false, //***remember to set to false */
      tabnameError: '',
      resultTabOptions: [
          { value: null, text: 'Choose ...' },
      ],
      operatorOptions: [
          { value: null, text: 'Choose ...' },
          { value: 'AND', text: 'AND' },
          { value: 'OR', text: 'OR' },
        ],
      ruleForm: {
        tabname: '',
        tabnameState: null,
        fields: [
          { key: 'movement', label: 'movement', visible: true },
          { key: 'negation', label: 'negation', visible: false },
          { key: 'resultTab', label: 'resultTab', visible: false },
          { key: 'resultTabDisplay', label: 'resultTabDisplay', visible: true },
          { key: 'operator', label: 'operator', visible: true },
          { key: 'showOperator', label: 'showOperator', visible: false},
          { key: 'operatorState', label: 'operatorState', visible: false},
          { key: 'delete', label: 'delete', visible: true },
          { key: 'edit', label: 'edit', visible: false },
        ],
        rulelist: [],
        currNegation: false,
        currResultTab: null,
        currResultTabState: null,
        currIndex: -1,
        displayString: '',
        validationDone: false,
        showDialogError: false,
        dialogError: '',
      },
    };
  },
  components: {
  // alert: Alert,
  },
  computed: {
    //rows() {
    //  this.totalRows=this.items.length
    //}
    visibleFields() {
      return this.ruleForm.fields.filter(field => field.visible)
    },
    bc(){
      return this.background || (this.dark ? 'rgba(0,0,0,0.8)' : 'rgba(255,255,255,0.8)')
    },
    bcFull(){
      return this.background || (this.dark ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.2)')
    },
  },
  mounted(){
  },
  methods: {
    //methods for configuring rule dialog
    moveRuleUp(x) {
      console.log('moveRuleUp ='+x)
      if(x > 0)
      {
        let eleToMoveUp = this.ruleForm.rulelist[x]
        eleToMoveUp.showOperator = true
        this.ruleForm.rulelist.splice(x, 1)
        this.ruleForm.rulelist.splice(x-1,0,eleToMoveUp)
        this.ruleForm.rulelist[this.ruleForm.rulelist.length-1].showOperator = false
        if(this.ruleForm.validationDone)
          this.getNullOperators(this.ruleForm.rulelist)
      }
    },
    moveRuleDown(x) {
      console.log('moveRuleDown ='+x)
      if(x < this.ruleForm.rulelist.length-1)
      {
        let eleToMoveDown = this.ruleForm.rulelist[x]
        eleToMoveDown.showOperator = true
        this.ruleForm.rulelist.splice(x, 1)
        this.ruleForm.rulelist[this.ruleForm.rulelist.length-1].showOperator = true
        this.ruleForm.rulelist.splice(x+1,0,eleToMoveDown)
        this.ruleForm.rulelist[this.ruleForm.rulelist.length-1].showOperator = false
        if(this.ruleForm.validationDone)
          this.getNullOperators(this.ruleForm.rulelist)
      }
    },
    editRule(x) {
      console.log('editRule ='+x)
      this.ruleForm.rulelist[x].edit = true
      this.ruleForm.currIndex = x
      this.ruleForm.currResultTab = this.ruleForm.rulelist[x].resultTab
      this.ruleForm.currNegation = this.ruleForm.rulelist[x].negation
    },
    clearEditRule() {
      console.log('clearEditRule')
      this.ruleForm.rulelist[this.ruleForm.currIndex].edit = false
      this.ruleForm.currIndex = -1
      this.resetAddNewRule()
    },
    confirmEditRule(evt) {
      console.log('confirmEditRule')
      if(this.ruleForm.currResultTab!=null)
      {
        this.ruleForm.rulelist[this.ruleForm.currIndex].resultTab = this.ruleForm.currResultTab
        this.ruleForm.rulelist[this.ruleForm.currIndex].negation = this.ruleForm.currNegation
        this.getResultTabDisplayString()
        this.ruleForm.rulelist[this.ruleForm.currIndex].resultTabDisplay = this.ruleForm.displayString
        this.ruleForm.rulelist[this.ruleForm.currIndex].edit = false
        this.ruleForm.currIndex = -1
        this.resetAddNewRule()
      }
      else
        evt.preventDefault();//needed to address "Form submission canceled because the form is not connected"
      console.log(this.ruleForm.rulelist)
    },
    deleteRule(x) {
      console.log('deleteRule ='+x)
      this.ruleForm.rulelist.splice(x, 1)
      if(this.ruleForm.rulelist.length > 0)
        this.ruleForm.rulelist[this.ruleForm.rulelist.length-1].showOperator = false
    },
    //methods for configuring new tab view of combined results
    closeTab(x) {
      console.log('closeTab ='+x)
      let tabToClose = ''
      for (let i = 0; i < this.gdssearch.tabheader.length; i++) {
        console.log('this.gdssearch.tabheader[i]='+this.gdssearch.tabheader[i])
        if (this.gdssearch.tabheader[i] == x) {
          console.log('found tab!')
          tabToClose = this.gdssearch.tabheader[i]
          this.gdssearch.tabheader.splice(i, 1)
          this.validItems.splice(i, 1)
        }
      }
      this.totalValidTabs--
      for (let i = 0; i < this.combinedResultTabList.length; i++) {
        if (this.combinedResultTabList[i] == x) {
          this.combinedResultTabList.splice(i, 1)
        }
      }
    },
    clearDialog(){
      this.ruleForm.tabname = ''
      this.ruleForm.tabnameState = null
      this.ruleForm.rulelist = []
      this.ruleForm.currIndex = -1
      this.ruleForm.displayString = ''
      this.ruleForm.validationDone = false
      this.resetAddNewRule()
    },
    getResultTabDisplayString() {
      let maxlength = 30
      this.ruleForm.displayString = ''
      let selectedTabString = this.resultTabOptions[this.ruleForm.currResultTab+1].text
      if(selectedTabString.length>maxlength)
        this.ruleForm.displayString = this.ruleForm.displayString + selectedTabString.substr(0, maxlength) + '...'
      else
        this.ruleForm.displayString = this.ruleForm.displayString + selectedTabString
      console.log("displayString="+this.ruleForm.displayString)
    },
    resetAddNewRule(){
      this.ruleForm.currResultTab = null
      this.ruleForm.currNegation = false
      this.ruleForm.dialogError = ''
      this.ruleForm.showDialogError = false
      this.ruleForm.currResultTabState = null
    },
    validateResultTab() {
      if(this.ruleForm.currResultTab == null)
        this.ruleForm.currResultTabState = false
      else
        this.ruleForm.currResultTabState = true
      console.log("[validateResultTab] currResultTabState="+this.ruleForm.currResultTabState)
    },
    addRule(){
      console.log("addRule pressed")
      this.validateResultTab()
      if(this.ruleForm.currResultTab != null)//do something - add to ruleList
      {
        this.getResultTabDisplayString()
        console.log("this.ruleForm.currResultTab="+this.ruleForm.currResultTab)
        const currRule = {
          movement: false,
          negation: this.ruleForm.currNegation,
          resultTab: this.ruleForm.currResultTab,
          resultTabDisplay: this.ruleForm.displayString,
          operator: null,
          showOperator: false,
          operatorState: null,
          delete: false,
          edit: false
        }
        if(this.ruleForm.rulelist.length > 0)
          this.ruleForm.rulelist[this.ruleForm.rulelist.length-1].showOperator = true
        this.ruleForm.rulelist.push(currRule)
        //reset ruleForm
        this.resetAddNewRule()
      }
      console.log("addRule finished")
      console.log(this.ruleForm.rulelist)
    },
    getNullOperators: function(items) {
      return items.filter(function(item, index) {
        if(item.operator == null)
          item.operatorState = false
        else
          item.operatorState = true
        if(index!=items.length-1)
          return item.operator == null
      })
    },
    getCurrOperatorState(currOperator, index) {
      if(currOperator!=null)
        this.ruleForm.rulelist[index].operatorState = true
      else
      {
        if(index==this.ruleForm.rulelist.length-1)
          this.ruleForm.rulelist[index].operatorState = true
        else
          this.ruleForm.rulelist[index].operatorState = false
      }
    },
    validateTabname() {
      if(this.ruleForm.tabname == "" || this.combinedResultTabList.includes(this.ruleForm.tabname))
      {
        if(this.ruleForm.tabname == "")
          this.tabnameError = 'Tab name must not be empty!'
        else
          this.tabnameError = 'Duplicate tab names detected! Please enter a new tab name.'
        this.ruleForm.tabnameState = false
      }
      else
        this.ruleForm.tabnameState = true
    },
    createNewTab(evt) {  
      //check if all rules except last rule has defined operators
      this.validateTabname()
      if(this.ruleForm.rulelist.length==0)
      {
        this.ruleForm.dialogError = 'Please add a valid rule.'
        this.ruleForm.showDialogError = true
      }
      this.validationDone = true
      let items = this.ruleForm.rulelist
      let nullOperators = this.getNullOperators(items)
      let validOperators = false
      console.log("nullOperators="+nullOperators)
      if(nullOperators.length==0)
        validOperators = true
      else
        validOperators = false
      console.log("validOperators="+validOperators)
      if(validOperators && this.ruleForm.tabnameState && this.ruleForm.rulelist.length>0){
        //evt.preventDefault();//needed to address "Form submission canceled because the form is not connected"
        this.gdssearch.tabheader.push(this.ruleForm.tabname)
        this.combinedResultTabList.push(this.ruleForm.tabname)
        this.totalValidTabs++
        console.log("this.gdssearch.tabheader=")
        console.log(this.gdssearch.tabheader)
        console.log("this.combinedResultTabList="+this.combinedResultTabList)
        //console.log("this.tabIndex=["+ this.tabIndex+"]")
        //console.log("this.totalValidTabs=["+ this.totalValidTabs+"]")
        if(this.totalValidTabs == 1)
          this.isCombinedResult = true
        this.$refs.modalResultCombi.hide()
        
        // getting the correct data to push to validItems
        let newValidItems = []
        this.loading=true
        console.log(items)
        // extract needed field of rulelist to be transferred
        // note that null value in field need to be specifically handled within json...if not the backend will
        // get null or zero values in fields when converting from json
        let jsonRulelist = []
        items.forEach(element => {
          let op = ''
          if(element.operator!=null)
            op = element.operator
          let e = {
            negation: element.negation,
            resultTabIndex: element.resultTab,
            operator: op
          }
          jsonRulelist.push(e)
        });
        console.log("jsonRulelist=")
        console.log(jsonRulelist)
        let jsonValidResultTab = []
        this.resultTabOptions.forEach(element => {
          if(element.value!=null)
            jsonValidResultTab.push(element.text)
        });
        this.busytext="Retrieving records for "+this.ruleForm.tabname+". Please wait..."
        
        let paramInput = {
          ruleListArray: jsonRulelist, 
          validItemsArray: this.validItems, 
          validTabArray: jsonValidResultTab
        }

        api.getNewTabRecords(JSON.stringify(paramInput)).then(response => {
          console.log("done transferRulelist...")
          this.validItems.push(response.data)
          this.loading=false
        })
        this.clearDialog()
      }
      else
        evt.preventDefault();//needed to address "Form submission canceled because the form is not connected"
    },
    //methods for tab for invalid records
    viewInvalid() {
      console.log("viewInvalid")
      this.loading=true
      this.busytext="Retrieving invalid records. Please wait..."
      api.getInvalidRecords().then(response => {
        console.log("done getInvalidRecords, create new tab to display invalid records")
        this.invalidItems = response.data;
        if(response.data.length==0) 
        {
          this.systemmsg = "There are no invalid records."
          this.setTextMessageClassUsingIsDanger(false)
        }
        else
        {
          this.systemmsg = ""
          this.setTextMessageClassUsingIsDanger(false)
        }
        this.computeInvalidRows();
        this.loading=false
        this.showInvalid=true;
        this.$nextTick(() => {
          this.tabIndex=0
          console.log("done this.tabIndex="+this.tabIndex)
        });
      })
      // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.show = true;
      });
    },
    computeInvalidRows() {
      this.$nextTick(() => {
        console.log("computeInvalidRows")
        this.totalRows=this.invalidItems.length;
        console.log("this.totalRows="+this.totalRows)
      });
    },
    onInvalidRowSelected(items) {
      this.$nextTick(() => {
        this.invalidSelected = items
        console.log("onInvalidRowSelected this.invalidSelected.length="+this.invalidSelected.length)
        console.log("onInvalidRowSelected this.invalidSelected="+this.invalidSelected)
      })
    },
    selectAllInvalidRows() {
      this.$nextTick(() => {
        this.$refs.selectableInvalidTable.selectAllRows()
      })
    },
    clearInvalidSelected() {
      this.$nextTick(() => {
        this.$refs.selectableInvalidTable.clearSelected()
      })
    },
    setAsValidRows(items) {
      console.log("setAsValidRows items="+items)
      this.$nextTick(() => {
        console.log("setAsValidRows items.length="+items.length)
        if(items.length==0)//no rows selected
        {
          this.systemmsg = "Please select one or more rows of invalid records that you wish to set as valid records."
          this.setTextMessageClassUsingIsDanger(false)
        }
        else{//some rows selected, check if confirmed proceeding to setting them as valid
          this.systemmsg = "The selected invalid records will be set as valid.\n Please click the CONFIRM button if you wish to proceed."
          this.setTextMessageClassUsingIsDanger(true)
          this.confirmSetAsValid=true
        }
      })
    },
    confirmSetAsValidRows(items) {
      console.log("confirmSetAsValidRows items.length="+items.length)
      this.loading=true
      this.busytext="Setting selected invalid records as valid. Please wait..."
      let jsonObject=[]
      items.forEach(element => {
        //update invalid tabs by removing selected invalid entries
        this.invalidItems.splice(this.invalidItems.indexOf(element), 1)
        //save these selected entries to jsonObject to be passed to api that updates postgres DB 
        jsonObject.push(element)
      });
      console.log("confirmSetAsValidRows jsonObject="+jsonObject)
      this.computeInvalidRows();
      console.log("confirmSetAsValidRows computeInvalidRows")
      
      api.setInvalidRecordsAsValid(JSON.stringify(jsonObject)).then(response => {
        console.log("done confirmSetAsValidRows")
        let resData = response.data;
        console.log("response.data length = "+response.data.length+" resData.length="+resData.length)
        let itemArray = [];
        if(resData.length>0){
          resData.map((data, index)=> {
            console.log("data length = "+data.length+" index="+index)
            console.log("data = "+data)
            if(data.length>0){
              //update the invalid records entries to corresponding valid tabs
              itemArray.push(data);
              //update message details regarding number of valid records
              console.log("gdssearch message="+this.gdssearch.message)
              let msgArr=this.gdssearch.message.split("\n")
              let currMsgArrIndex=resData.length+index+1
              console.log("gdssearch message currMsgArrIndex="+currMsgArrIndex)
              console.log("gdssearch message curr="+msgArr[currMsgArrIndex])
              let msgDelimiter=" validated GDS records for "
              let msgDelimiterIndex=msgArr[currMsgArrIndex].indexOf(msgDelimiter)
              let currMsgPostfix=msgArr[currMsgArrIndex].substring(msgDelimiterIndex+1)
              let currMsgPrefix=data.length
              msgArr[currMsgArrIndex]=currMsgPrefix+" "+currMsgPostfix
              let updateMsg=""
              let counter=0
              msgArr.forEach(msgArrElement => {
                updateMsg=updateMsg+msgArrElement
                if(counter<msgArr.length)
                  updateMsg=updateMsg+"\n"
                counter++
              });
              this.gdssearch.message=updateMsg
              //console.log("currMsgPrefix="+currMsgPrefix)
              //console.log("currMsgPostfix="+currMsgPostfix)
            }
          })
        }
        this.validItems=itemArray;
        this.loading=false
        this.showInvalid=true;
      })
      this.loading=false  
      this.show = false;
      this.$nextTick(() => {
        this.systemmsg=""
        this.setTextMessageClassUsingIsDanger(false)
        this.show = true;
        this.confirmSetAsValid=false
      });
    },
    setTextMessageClassUsingIsDanger(flag)
    {
      this.isDanger=flag
      this.isInfo=!flag
    },
    //methods for other tabs
    savelist : function(items, filename) {
      const data = XLSX.utils.json_to_sheet(items)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, data, 'data')
      XLSX.writeFile(wb,filename+'.xlsx')
    },
    download(items, foldername){
      this.$nextTick(() => {
        console.log("download items="+items)
        console.log("download items.length="+items.length+" foldername="+foldername)
        if(items.length==0)//no rows selected
        {
          this.systemmsg = "Please select one or more rows of valid records to download SOFT files."
          this.setTextMessageClassUsingIsDanger(false)
        }
        else{
          this.loading=true
          this.busytext="Downloading SOFT files to directory. This may take a few minutes. Please wait..."
          let jsonObject=[]
          if(items.length>0){
            items.forEach(function(itemElement, index) {
              jsonObject.push(itemElement)
            })
          }
          console.log("download jsonObject.length="+jsonObject.length)
          api.setDownloadFoldername(foldername).then(async response => { 
            try{
              let response = await api.downloadSOFT(JSON.stringify(jsonObject))
              this.loading = true
              console.log("response after await: " +response.data);
              console.log("response after await response data length: " +response.data.length);
              if(response.data.length>0)
              {
                this.loading = false
                this.systemmsg = response.data;
                this.setTextMessageClassUsingIsDanger(false)
              }
            }
            catch (err) {
              console.log("Error: " +err);
              this.loading = false
            }
          })
          /*api.setDownloadFoldername(foldername).then(response => { 
		        api.downloadSOFT(JSON.stringify(jsonObject)).then(response => {
              console.log("download api.downloadSOFT done")
              this.loading=false
              if(!response)
              {
                this.systemmsg = response.data;
                this.setTextMessageClassUsingIsDanger(false)
              }
            })
            .catch(e=>{
              console.log("Error: " +e);
              this.loading=false
              this.systemmsg = "File download still require sometime and will be continuing in the background.";
              this.setTextMessageClassUsingIsDanger(false)
            })
          })*/
        }
      })
    },
    computeRows(key) {//this method will be called every time we click on a tab
      this.$nextTick(() => {
        console.log("computeRows key="+key)
        if(this.combinedResultTabList.includes(this.gdssearch.tabheader[key]))
        {
          this.isCombinedResult=true
          this.tabFields = this.validItems_fields_combined
        }
        else
        {
          this.isCombinedResult=false
          this.tabFields = this.validItems_fields
        }
        if(this.totalValidTabs > 0)
          this.hasResults = true
        else
          this.hasResults = false
        this.totalRows=this.validItems[key].length
        console.log("this.totalRows="+this.totalRows)
        if(this.totalRows==0) 
        {
          this.systemmsg = "There are no GDS records found."
          this.setTextMessageClassUsingIsDanger(false)
        }
        else
        {
          this.systemmsg = ""
          this.setTextMessageClassUsingIsDanger(false)
        }
      });
    },
    setAsInvalidRows(items) {
      this.$nextTick(() => {
        console.log("setAsInvalidRows items="+items)
        console.log("setAsInvalidRows items.length="+items.length)
        if(items.length==0)//no rows selected
        {
          this.systemmsg = "Please select one or more rows of valid records that you wish to set as invalid records."
          this.setTextMessageClassUsingIsDanger(false)
        }
        else{//some rows selected, check if confirmed proceeding to setting them as invalid
          var invalidReasonSet=true;
          items.map((item, index)=> {
            if(item.length>0){
              console.log("item.length="+item.length)
              console.log("item="+item)
              item.forEach(function(itemElement, index) {
                console.log("itemElement="+itemElement)
                console.log("setAsInvalidRows itemElement.validity="+itemElement['validity'])
                if(itemElement['validity']=="Valid")
                  invalidReasonSet=false;
              })
            }
          })
          console.log("setAsInvalidRows invalidReasonSet="+invalidReasonSet)
          if(invalidReasonSet==false)
          {
            this.systemmsg = "Please set the reason why they are invalid."
            this.setTextMessageClassUsingIsDanger(false)
          }
          else{
            this.systemmsg = "The selected valid records will be set as invalid.\n Please click the CONFIRM button if you wish to proceed."
            this.setTextMessageClassUsingIsDanger(true)
            this.confirmSetAsInvalid=true
          }
        }
      })
    },
    confirmSetAsInvalidRows(selectedArr, arr, key, numTabs) {
      console.log("confirmSetAsInvalidRows selectedArr.length="+selectedArr.length+" numTabs="+numTabs)
      console.log("confirmSetAsInvalidRows selectedArr="+selectedArr)
      this.loading=true
      this.busytext="Setting selected valid records as invalid. Please wait..."
      let jsonObject=[]
      selectedArr.forEach(function(selectedArritem, index) {
        jsonObject.push(selectedArritem)
        arr.splice(arr.indexOf(selectedArritem), 1)
      })
      console.log("confirmSetAsInvalidRows jsonObject="+jsonObject)
      this.computeRows(key);
      console.log("confirmSetAsInvalidRows computeRows of "+key)
      
      api.setValidRecordsAsInvalid(JSON.stringify(jsonObject)).then(response => {
        console.log("done confirmSetAsInvalidRows")
        console.log("response.data.length="+response.data.length)
        console.log("response.data="+response.data)
        this.invalidItems = response.data;
        this.computeInvalidRows();
        //update message details regarding number of valid records
        console.log("gdssearch message="+this.gdssearch.message)
        let msgArr=this.gdssearch.message.split("\n")
        let currMsgArrIndex=parseInt(numTabs)+parseInt(key)+1
        console.log("gdssearch message currMsgArrIndex="+currMsgArrIndex)
        console.log("gdssearch message curr="+msgArr[currMsgArrIndex])
        let msgDelimiter=" validated GDS records for "
        let msgDelimiterIndex=msgArr[currMsgArrIndex].indexOf(msgDelimiter)
        let currMsgPrefix=msgArr[currMsgArrIndex].substring(0,msgDelimiterIndex)
        let currMsgPostfix=msgArr[currMsgArrIndex].substring(msgDelimiterIndex+1)
        let currMsgPrefixInt=parseInt(currMsgPrefix)-selectedArr.length
        msgArr[currMsgArrIndex]=currMsgPrefixInt+" "+currMsgPostfix
        let updateMsg=""
        let counter=0
        msgArr.forEach(msgArrElement => {
          updateMsg=updateMsg+msgArrElement
          if(counter<msgArr.length)
            updateMsg=updateMsg+"\n"
          counter++
        });
        this.gdssearch.message=updateMsg
        this.loading=false
        this.showInvalid=true;
      })
      
      this.show = false;
      this.$nextTick(() => {
        this.systemmsg=""
        this.setTextMessageClassUsingIsDanger(false)
        this.show = true;
        this.confirmSetAsInvalid=false
      });
    },
    onRowSelected(index, items) {
      //this.currtab_selected = this.selected[index]
      // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.selected[index] = items
        this.show = true;
      });
    },
    selectAllRows(index) {
      this.$nextTick(() => {
        this.$refs.selectableTable[index].selectAllRows()
      })
            // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.show = true;
      });
    },
    clearSelected(index) {
      this.$nextTick(() => {
        this.$refs.selectableTable[index].clearSelected()
      })
            // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.show = true;
      });
    },
    onSubmit(evt) {
      evt.preventDefault();//needed to address "Form submission canceled because the form is not connected"
      this.form.organism = '';
      this.form.disease = '';
      this.form.anatomy = '';
      this.form.information = '';
      this.show_information = false;
      this.gdssearch.message = '';
      this.systemmsg = "";
      this.setTextMessageClassUsingIsDanger(false);
      //summary of search keywords
      this.loading = true
      this.showInvalid=false;
      this.busytext = 'Summarizing keywords. Please wait...',
      api.processKeyword(this.form.keyword).then(response => {
          this.loading = false
          //console.log('process done ....now back at response of processKeyword')
          this.backendResponse = response.data;
          console.log(response.data)
          if(this.backendResponse[0]=="true"){
            this.form['organism'] = this.backendResponse[1];
            this.form['disease'] = this.backendResponse[2];
            this.form['anatomy'] = this.backendResponse[3];
            this.gdssearch.message = '';

            this.loading = true
            this.busytext = 'Performing programmatic access to retrieve records\n from Gene Expression Omnibus. Please wait...',
            api.getGeoOmnibusGDS(this.form.keyword).then(response => {
              this.loading = false
              this.backendResponse = response.data;
              console.log(response.data)
              if(this.backendResponse[0]=="true")
              {
                var message = this.backendResponse[2];
                this.gdssearch.gdssize = this.backendResponse[1];
                this.gdssearch.tabheader = this.backendResponse[3];
                this.gdssearch.message = 'Search complete successfully!'+message;
                this.tabIndex=this.gdssearch.tabheader.length;
                this.totalValidTabs=this.gdssearch.tabheader.length;
                console.log("totalValidTabs "+this.totalValidTabs)
                let header = this.gdssearch.tabheader
                if(header.length>0) {
                  header.map((data, index)=> {
                    let optionItem = { value: index, text: data }
                    this.resultTabOptions.push(optionItem);
                  })
                }
                console.log("resultTabOptions="+this.resultTabOptions)
              }
              
              this.loading = true
              this.busytext = 'Parsing and loading GDS records from Gene Expression Omnibus.\n This may take a couple of minutes. Please wait...',
              api.populateDBWithGDS().then(response => {
                console.log("done populateDBWithGDS "+response.data)
                let itemArray = [];
                let resData = response.data;
                console.log("response.data length = "+response.data.length+" resData.length="+resData.length)
                if(resData.length>0){
                  resData.map((data, index)=> {
                    //console.log("data length = "+data.length+" index="+index)
                    //console.log("data = "+data)
                    if(data.length>=0){
                      //console.log("before JSON parse data = "+data)
                      itemArray.push(data);
                      //if(index==0 && data.length==0) 
                      //{
                      //  this.systemmsg = "There are no GDS records found."
                      //  this.setTextMessageClassUsingIsDanger(false)
                      //}
                    }
                  })
                }
                this.validItems=itemArray;
                this.loading=false
                api.getNumValidatedRecords().then(response => {
                  console.log("done getNumValidatedRecords ")
                  this.gdssearch.message = this.gdssearch.message+response.data;
                  this.computeRows(0);
                })
              })
              .catch(e=>{
                console.log("Error: " +e.response);
              })
            })
          }
          else
          {
            this.form['information'] = " Timeout when accessing external repositories during summarization of search keywords."
            this.show_information = true;
            this.gdssearch.message = '';
          }
      })
      .catch(error => {
        //this.errors.push(error)
        console.log("Error: " + error);
      })
      // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.show = true;
      });
    },
    async initDBTable() {
      this.gdssearch.message = 'Initializing DB table. Please wait...';
      try{
        let response = await api.initDBTable()
        console.log("initDBTable response after await: " +response.data)
        if(response.data.length>0)
        {
          this.disableSubmit = false
          this.gdssearch.message = response.data
        }
      }
      catch (err) {
        console.log("Error: " +err);
      }
    },
    async setupPython() {
      this.gdssearch.message = 'Setting up NLP pipeline. Please wait...';
      try{
        let response = await api.setupPython()
        console.log("setupPython response after await: " +response.data)
        if(response.data.length>0)
        {
          this.disableSubmit = false
          this.gdssearch.message = response.data
        }
      }
      catch (err) {
        console.log("Error: " +err);
      }
    },
    onReset(event) {
      event.preventDefault();
      // Reset our form values
      this.form.keyword = '';
      this.form.organism = '';
      this.form.disease = '';
      this.form.anatomy = '';
      this.form.information = '';
      this.show_information = false;
      this.gdssearch.message = '';
      this.systemmsg = '';
      this.disableSubmit = false
      this.tabFields = this.validItems_fields
      this.tabIndex = 1
      this.isCombinedResult = false
      this.combinedResultTabList = []
      this.hasResults = false //***remember to set to fal
      this.resultTabOptions = [
        { value: null, text: 'Choose ...' },
      ],
      // Trick to reset/clear native browser form validation state
      this.show = false;
      this.$nextTick(() => {
        this.show = true;
      });
    },
  },
  created() {
    // initialize value of organism, disease and anatomy
    this.form.organism = '';
    this.form.disease = '';
    this.form.anatomy = '';
    this.form.information = '';
    this.show_information = false;
    this.showInvalid=false;
    this.gdssearch.message = '';
    this.systemmsg = '';
    this.disableSubmit = false
    this.tabFields = this.validItems_fields
    this.tabIndex = 1
    this.isCombinedResult = false
    this.combinedResultTabList = []
    this.hasResults = false //***remember to set to false */
    this.clearDialog()
    this.resultTabOptions = [
      { value: null, text: 'Choose ...' },
    ]  
    //this.disableSubmit = true;
    //this.initDBTable();
    //this.setupPython();
  },
};
</script>
<style scoped src="../assets/css/MainPage.css">
</style>

