export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  graphql_public: {
    Tables: {
      [_ in never]: never
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      graphql: {
        Args: {
          extensions?: Json
          operationName?: string
          query?: string
          variables?: Json
        }
        Returns: Json
      }
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
  public: {
    Tables: {
      accounts: {
        Row: {
          archived: boolean
          closing_day: number | null
          color: string | null
          created_at: string
          credit_limit: number | null
          currency: string
          due_day: number | null
          id: string
          institution: string | null
          last4: string | null
          name: string
          opening_balance: number
          type: Database["public"]["Enums"]["account_type"]
          updated_at: string
          workspace_id: string
        }
        Insert: {
          archived?: boolean
          closing_day?: number | null
          color?: string | null
          created_at?: string
          credit_limit?: number | null
          currency?: string
          due_day?: number | null
          id?: string
          institution?: string | null
          last4?: string | null
          name: string
          opening_balance?: number
          type?: Database["public"]["Enums"]["account_type"]
          updated_at?: string
          workspace_id?: string
        }
        Update: {
          archived?: boolean
          closing_day?: number | null
          color?: string | null
          created_at?: string
          credit_limit?: number | null
          currency?: string
          due_day?: number | null
          id?: string
          institution?: string | null
          last4?: string | null
          name?: string
          opening_balance?: number
          type?: Database["public"]["Enums"]["account_type"]
          updated_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "accounts_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      budget_alerts: {
        Row: {
          budget_id: string
          id: string
          level: Database["public"]["Enums"]["alert_level"]
          triggered_at: string
          workspace_id: string
        }
        Insert: {
          budget_id: string
          id?: string
          level: Database["public"]["Enums"]["alert_level"]
          triggered_at?: string
          workspace_id: string
        }
        Update: {
          budget_id?: string
          id?: string
          level?: Database["public"]["Enums"]["alert_level"]
          triggered_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "budget_alerts_budget_fkey"
            columns: ["budget_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "budget_progress"
            referencedColumns: ["budget_id", "workspace_id"]
          },
          {
            foreignKeyName: "budget_alerts_budget_fkey"
            columns: ["budget_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "budgets"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "budget_alerts_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      budgets: {
        Row: {
          category_id: string | null
          created_at: string
          id: string
          limit_amount: number
          month: string | null
          scope: Database["public"]["Enums"]["budget_scope"]
          subcategory_id: string | null
          updated_at: string
          workspace_id: string
        }
        Insert: {
          category_id?: string | null
          created_at?: string
          id?: string
          limit_amount: number
          month?: string | null
          scope: Database["public"]["Enums"]["budget_scope"]
          subcategory_id?: string | null
          updated_at?: string
          workspace_id?: string
        }
        Update: {
          category_id?: string | null
          created_at?: string
          id?: string
          limit_amount?: number
          month?: string | null
          scope?: Database["public"]["Enums"]["budget_scope"]
          subcategory_id?: string | null
          updated_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "budgets_category_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "budgets_subcategory_fkey"
            columns: ["subcategory_id", "category_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "category_id"]
          },
          {
            foreignKeyName: "budgets_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      categories: {
        Row: {
          archived: boolean
          color: string
          created_at: string
          icon: string | null
          id: string
          kind: Database["public"]["Enums"]["transaction_type"]
          name: string
          updated_at: string
          workspace_id: string
        }
        Insert: {
          archived?: boolean
          color?: string
          created_at?: string
          icon?: string | null
          id?: string
          kind?: Database["public"]["Enums"]["transaction_type"]
          name: string
          updated_at?: string
          workspace_id?: string
        }
        Update: {
          archived?: boolean
          color?: string
          created_at?: string
          icon?: string | null
          id?: string
          kind?: Database["public"]["Enums"]["transaction_type"]
          name?: string
          updated_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "categories_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      goal_contributions: {
        Row: {
          account_id: string | null
          amount: number
          contributed_at: string
          created_at: string
          goal_id: string
          id: string
          note: string | null
          user_id: string
          workspace_id: string
        }
        Insert: {
          account_id?: string | null
          amount: number
          contributed_at?: string
          created_at?: string
          goal_id: string
          id?: string
          note?: string | null
          user_id?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string | null
          amount?: number
          contributed_at?: string
          created_at?: string
          goal_id?: string
          id?: string
          note?: string | null
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "goal_contributions_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "goal_contributions_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "goal_contributions_goal_fkey"
            columns: ["goal_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "saving_goals"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "goal_contributions_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      imports: {
        Row: {
          created_at: string
          file_name: string
          id: string
          kind: string
          status: string
          storage_path: string | null
          uploaded_by: string
          workspace_id: string
        }
        Insert: {
          created_at?: string
          file_name: string
          id?: string
          kind?: string
          status?: string
          storage_path?: string | null
          uploaded_by?: string
          workspace_id?: string
        }
        Update: {
          created_at?: string
          file_name?: string
          id?: string
          kind?: string
          status?: string
          storage_path?: string | null
          uploaded_by?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "imports_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      inbound_emails: {
        Row: {
          body_text: string | null
          created_at: string
          error: string | null
          from_address: string
          id: string
          message_id: string
          parse_status: Database["public"]["Enums"]["email_parse_status"]
          parser: string | null
          received_at: string
          subject: string | null
          transaction_id: string | null
          user_id: string
          workspace_id: string
        }
        Insert: {
          body_text?: string | null
          created_at?: string
          error?: string | null
          from_address: string
          id?: string
          message_id: string
          parse_status?: Database["public"]["Enums"]["email_parse_status"]
          parser?: string | null
          received_at?: string
          subject?: string | null
          transaction_id?: string | null
          user_id: string
          workspace_id: string
        }
        Update: {
          body_text?: string | null
          created_at?: string
          error?: string | null
          from_address?: string
          id?: string
          message_id?: string
          parse_status?: Database["public"]["Enums"]["email_parse_status"]
          parser?: string | null
          received_at?: string
          subject?: string | null
          transaction_id?: string | null
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "inbound_emails_transaction_id_workspace_id_fkey"
            columns: ["transaction_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "transactions"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "inbound_emails_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      ingest_addresses: {
        Row: {
          created_at: string
          id: string
          rotated_at: string | null
          token: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          created_at?: string
          id?: string
          rotated_at?: string | null
          token?: string
          user_id: string
          workspace_id: string
        }
        Update: {
          created_at?: string
          id?: string
          rotated_at?: string | null
          token?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "ingest_addresses_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      installment_plans: {
        Row: {
          account_id: string
          category_id: string | null
          created_at: string
          description: string
          first_charge_on: string
          id: string
          installment_amount: number
          installments: number
          interest_rate: number | null
          merchant: string | null
          note: string | null
          purchased_on: string
          subcategory_id: string | null
          total_amount: number
          updated_at: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          account_id: string
          category_id?: string | null
          created_at?: string
          description: string
          first_charge_on: string
          id?: string
          installment_amount: number
          installments: number
          interest_rate?: number | null
          merchant?: string | null
          note?: string | null
          purchased_on?: string
          subcategory_id?: string | null
          total_amount: number
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string
          category_id?: string | null
          created_at?: string
          description?: string
          first_charge_on?: string
          id?: string
          installment_amount?: number
          installments?: number
          interest_rate?: number | null
          merchant?: string | null
          note?: string | null
          purchased_on?: string
          subcategory_id?: string | null
          total_amount?: number
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "installment_plans_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_category_id_workspace_id_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_subcategory_id_category_id_fkey"
            columns: ["subcategory_id", "category_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "category_id"]
          },
          {
            foreignKeyName: "installment_plans_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      merchant_rules: {
        Row: {
          account_id: string | null
          category_id: string | null
          created_at: string
          id: string
          pattern: string
          priority: number
          subcategory_id: string | null
          updated_at: string
          workspace_id: string
        }
        Insert: {
          account_id?: string | null
          category_id?: string | null
          created_at?: string
          id?: string
          pattern: string
          priority?: number
          subcategory_id?: string | null
          updated_at?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string | null
          category_id?: string | null
          created_at?: string
          id?: string
          pattern?: string
          priority?: number
          subcategory_id?: string | null
          updated_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "merchant_rules_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "merchant_rules_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "merchant_rules_category_id_workspace_id_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "merchant_rules_subcategory_id_category_id_fkey"
            columns: ["subcategory_id", "category_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "category_id"]
          },
          {
            foreignKeyName: "merchant_rules_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      monthly_reports: {
        Row: {
          generated_at: string
          id: string
          month: string
          storage_path: string | null
          workspace_id: string
        }
        Insert: {
          generated_at?: string
          id?: string
          month: string
          storage_path?: string | null
          workspace_id?: string
        }
        Update: {
          generated_at?: string
          id?: string
          month?: string
          storage_path?: string | null
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "monthly_reports_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      profiles: {
        Row: {
          created_at: string
          currency: string
          email: string | null
          full_name: string | null
          id: string
          updated_at: string
        }
        Insert: {
          created_at?: string
          currency?: string
          email?: string | null
          full_name?: string | null
          id: string
          updated_at?: string
        }
        Update: {
          created_at?: string
          currency?: string
          email?: string | null
          full_name?: string | null
          id?: string
          updated_at?: string
        }
        Relationships: []
      }
      reminders: {
        Row: {
          account_id: string | null
          amount: number | null
          category_id: string | null
          created_at: string
          due_date: string
          frequency: Database["public"]["Enums"]["recurring_frequency"] | null
          id: string
          is_done: boolean
          kind: Database["public"]["Enums"]["reminder_kind"]
          note: string | null
          remind_days_before: number
          title: string
          updated_at: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          account_id?: string | null
          amount?: number | null
          category_id?: string | null
          created_at?: string
          due_date: string
          frequency?: Database["public"]["Enums"]["recurring_frequency"] | null
          id?: string
          is_done?: boolean
          kind: Database["public"]["Enums"]["reminder_kind"]
          note?: string | null
          remind_days_before?: number
          title: string
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string | null
          amount?: number | null
          category_id?: string | null
          created_at?: string
          due_date?: string
          frequency?: Database["public"]["Enums"]["recurring_frequency"] | null
          id?: string
          is_done?: boolean
          kind?: Database["public"]["Enums"]["reminder_kind"]
          note?: string | null
          remind_days_before?: number
          title?: string
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "reminders_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "reminders_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "reminders_category_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "reminders_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      saving_goals: {
        Row: {
          account_id: string | null
          created_at: string
          currency: string
          current_amount: number
          deadline: string | null
          id: string
          name: string
          period: Database["public"]["Enums"]["goal_period"]
          target_amount: number
          updated_at: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          account_id?: string | null
          created_at?: string
          currency?: string
          current_amount?: number
          deadline?: string | null
          id?: string
          name: string
          period: Database["public"]["Enums"]["goal_period"]
          target_amount: number
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string | null
          created_at?: string
          currency?: string
          current_amount?: number
          deadline?: string | null
          id?: string
          name?: string
          period?: Database["public"]["Enums"]["goal_period"]
          target_amount?: number
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "saving_goals_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "saving_goals_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "saving_goals_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      subcategories: {
        Row: {
          category_id: string
          created_at: string
          id: string
          name: string
          updated_at: string
          workspace_id: string
        }
        Insert: {
          category_id: string
          created_at?: string
          id?: string
          name: string
          updated_at?: string
          workspace_id?: string
        }
        Update: {
          category_id?: string
          created_at?: string
          id?: string
          name?: string
          updated_at?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "subcategories_category_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "subcategories_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      transaction_attachments: {
        Row: {
          content_type: string | null
          created_at: string
          file_name: string
          id: string
          size_bytes: number | null
          storage_path: string
          transaction_id: string | null
          uploaded_by: string
          workspace_id: string
        }
        Insert: {
          content_type?: string | null
          created_at?: string
          file_name: string
          id?: string
          size_bytes?: number | null
          storage_path: string
          transaction_id?: string | null
          uploaded_by?: string
          workspace_id?: string
        }
        Update: {
          content_type?: string | null
          created_at?: string
          file_name?: string
          id?: string
          size_bytes?: number | null
          storage_path?: string
          transaction_id?: string | null
          uploaded_by?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "transaction_attachments_transaction_fkey"
            columns: ["transaction_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "transactions"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transaction_attachments_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      transactions: {
        Row: {
          account_id: string | null
          ai_confidence: number | null
          amount: number
          category_id: string | null
          created_at: string
          currency: string
          external_ref: string | null
          id: string
          installment_number: number | null
          installment_plan_id: string | null
          merchant: string | null
          note: string | null
          occurred_at: string
          raw_input: string | null
          reminder_id: string | null
          source: Database["public"]["Enums"]["capture_source"]
          status: Database["public"]["Enums"]["review_status"]
          subcategory_id: string | null
          type: Database["public"]["Enums"]["transaction_type"]
          updated_at: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          account_id?: string | null
          ai_confidence?: number | null
          amount: number
          category_id?: string | null
          created_at?: string
          currency?: string
          external_ref?: string | null
          id?: string
          installment_number?: number | null
          installment_plan_id?: string | null
          merchant?: string | null
          note?: string | null
          occurred_at?: string
          raw_input?: string | null
          reminder_id?: string | null
          source?: Database["public"]["Enums"]["capture_source"]
          status?: Database["public"]["Enums"]["review_status"]
          subcategory_id?: string | null
          type: Database["public"]["Enums"]["transaction_type"]
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Update: {
          account_id?: string | null
          ai_confidence?: number | null
          amount?: number
          category_id?: string | null
          created_at?: string
          currency?: string
          external_ref?: string | null
          id?: string
          installment_number?: number | null
          installment_plan_id?: string | null
          merchant?: string | null
          note?: string | null
          occurred_at?: string
          raw_input?: string | null
          reminder_id?: string | null
          source?: Database["public"]["Enums"]["capture_source"]
          status?: Database["public"]["Enums"]["review_status"]
          subcategory_id?: string | null
          type?: Database["public"]["Enums"]["transaction_type"]
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "transactions_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_account_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_category_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_installment_plan_fkey"
            columns: ["installment_plan_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "installment_plan_status"
            referencedColumns: ["installment_plan_id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_installment_plan_fkey"
            columns: ["installment_plan_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "installment_plans"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_reminder_fkey"
            columns: ["reminder_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "reminders"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_subcategory_category_fkey"
            columns: ["subcategory_id", "category_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "category_id"]
          },
          {
            foreignKeyName: "transactions_subcategory_fkey"
            columns: ["subcategory_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transactions_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      transfers: {
        Row: {
          amount: number
          created_at: string
          external_ref: string | null
          from_account_id: string
          id: string
          note: string | null
          occurred_at: string
          raw_input: string | null
          source: Database["public"]["Enums"]["capture_source"]
          status: Database["public"]["Enums"]["review_status"]
          to_account_id: string
          updated_at: string
          user_id: string
          workspace_id: string
        }
        Insert: {
          amount: number
          created_at?: string
          external_ref?: string | null
          from_account_id: string
          id?: string
          note?: string | null
          occurred_at?: string
          raw_input?: string | null
          source?: Database["public"]["Enums"]["capture_source"]
          status?: Database["public"]["Enums"]["review_status"]
          to_account_id: string
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Update: {
          amount?: number
          created_at?: string
          external_ref?: string | null
          from_account_id?: string
          id?: string
          note?: string | null
          occurred_at?: string
          raw_input?: string | null
          source?: Database["public"]["Enums"]["capture_source"]
          status?: Database["public"]["Enums"]["review_status"]
          to_account_id?: string
          updated_at?: string
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "transfers_from_account_id_workspace_id_fkey"
            columns: ["from_account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "transfers_from_account_id_workspace_id_fkey"
            columns: ["from_account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transfers_to_account_id_workspace_id_fkey"
            columns: ["to_account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "transfers_to_account_id_workspace_id_fkey"
            columns: ["to_account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "transfers_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      workspace_members: {
        Row: {
          created_at: string
          role: Database["public"]["Enums"]["workspace_role"]
          user_id: string
          workspace_id: string
        }
        Insert: {
          created_at?: string
          role?: Database["public"]["Enums"]["workspace_role"]
          user_id: string
          workspace_id: string
        }
        Update: {
          created_at?: string
          role?: Database["public"]["Enums"]["workspace_role"]
          user_id?: string
          workspace_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "workspace_members_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      workspaces: {
        Row: {
          created_at: string
          created_by: string
          currency: string
          id: string
          name: string
          updated_at: string
        }
        Insert: {
          created_at?: string
          created_by: string
          currency?: string
          id?: string
          name: string
          updated_at?: string
        }
        Update: {
          created_at?: string
          created_by?: string
          currency?: string
          id?: string
          name?: string
          updated_at?: string
        }
        Relationships: []
      }
    }
    Views: {
      account_balances: {
        Row: {
          account_id: string | null
          archived: boolean | null
          available_credit: number | null
          balance: number | null
          card_debt: number | null
          closing_day: number | null
          color: string | null
          credit_limit: number | null
          currency: string | null
          due_day: number | null
          institution: string | null
          last4: string | null
          name: string | null
          pending_installments: number | null
          type: Database["public"]["Enums"]["account_type"] | null
          workspace_id: string | null
        }
        Relationships: [
          {
            foreignKeyName: "accounts_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      budget_progress: {
        Row: {
          budget_id: string | null
          category_id: string | null
          is_standing: boolean | null
          limit_amount: number | null
          month: string | null
          percent_used: number | null
          remaining: number | null
          scope: Database["public"]["Enums"]["budget_scope"] | null
          spent: number | null
          subcategory_id: string | null
          workspace_id: string | null
        }
        Relationships: [
          {
            foreignKeyName: "budgets_category_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "budgets_subcategory_fkey"
            columns: ["subcategory_id", "category_id"]
            isOneToOne: false
            referencedRelation: "subcategories"
            referencedColumns: ["id", "category_id"]
          },
          {
            foreignKeyName: "budgets_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
      installment_plan_status: {
        Row: {
          account_id: string | null
          amount_remaining: number | null
          category_id: string | null
          description: string | null
          first_charge_on: string | null
          installment_amount: number | null
          installment_plan_id: string | null
          installments: number | null
          installments_posted: number | null
          installments_remaining: number | null
          merchant: string | null
          next_charge_on: string | null
          total_amount: number | null
          workspace_id: string | null
        }
        Relationships: [
          {
            foreignKeyName: "installment_plans_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "account_balances"
            referencedColumns: ["account_id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_account_id_workspace_id_fkey"
            columns: ["account_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_category_id_workspace_id_fkey"
            columns: ["category_id", "workspace_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id", "workspace_id"]
          },
          {
            foreignKeyName: "installment_plans_workspace_id_fkey"
            columns: ["workspace_id"]
            isOneToOne: false
            referencedRelation: "workspaces"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Functions: {
      ensure_personal_workspace: {
        Args: never
        Returns: {
          currency: string
          id: string
          name: string
        }[]
      }
      rotate_ingest_token: {
        Args: { p_workspace_id?: string }
        Returns: string
      }
    }
    Enums: {
      account_type:
        | "yape"
        | "cash"
        | "savings"
        | "credit_card"
        | "debit"
        | "wallet"
        | "investment"
        | "other"
      alert_level: "warning_80" | "exceeded_100"
      budget_scope: "global" | "category" | "subcategory"
      capture_source:
        | "manual"
        | "email"
        | "notification"
        | "statement"
        | "installment"
        | "ai"
      email_parse_status:
        | "received"
        | "parsed"
        | "unrecognized"
        | "duplicate"
        | "error"
      goal_period: "monthly" | "yearly"
      recurring_frequency: "weekly" | "monthly" | "yearly"
      reminder_kind: "payable" | "receivable"
      review_status: "confirmed" | "pending" | "ignored"
      transaction_type: "income" | "expense"
      workspace_role: "owner" | "admin" | "member" | "viewer"
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  graphql_public: {
    Enums: {},
  },
  public: {
    Enums: {
      account_type: [
        "yape",
        "cash",
        "savings",
        "credit_card",
        "debit",
        "wallet",
        "investment",
        "other",
      ],
      alert_level: ["warning_80", "exceeded_100"],
      budget_scope: ["global", "category", "subcategory"],
      capture_source: [
        "manual",
        "email",
        "notification",
        "statement",
        "installment",
        "ai",
      ],
      email_parse_status: [
        "received",
        "parsed",
        "unrecognized",
        "duplicate",
        "error",
      ],
      goal_period: ["monthly", "yearly"],
      recurring_frequency: ["weekly", "monthly", "yearly"],
      reminder_kind: ["payable", "receivable"],
      review_status: ["confirmed", "pending", "ignored"],
      transaction_type: ["income", "expense"],
      workspace_role: ["owner", "admin", "member", "viewer"],
    },
  },
} as const
